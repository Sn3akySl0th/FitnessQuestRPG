#!/usr/bin/env node

const path = require("path");

const PROJECT_ID = "fitquest-1f082";
const FIREBASE_TOOLS_ROOT = path.join(
  process.env.APPDATA || "",
  "npm",
  "node_modules",
  "firebase-tools"
);

function fieldValue(fields, name, fallback = null) {
  const value = fields?.[name];
  if (!value) return fallback;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.nullValue !== undefined) return null;
  return fallback;
}

function duplicateKey(entry) {
  const fields = entry?.mapValue?.fields || {};
  return [
    fieldValue(fields, "catalogId", 0),
    fieldValue(fields, "atk", 0),
    fieldValue(fields, "def", 0),
    fieldValue(fields, "hp", 0),
    fieldValue(fields, "rune1Id", 0) || 0,
    fieldValue(fields, "rune2Id", 0) || 0,
    fieldValue(fields, "upgradeLevel", 0),
    fieldValue(fields, "rarity", "COMMON"),
    fieldValue(fields, "traitIds", ""),
    fieldValue(fields, "originBiome", "") || ""
  ].join("|");
}

function equippedIds(document) {
  const character = document.fields?.character?.mapValue?.fields || {};
  const slots = ["weaponId", "headId", "chestId", "handsId", "legsId", "feetId", "trinketId"];
  return new Set(slots.map((slot) => fieldValue(character, slot)).filter((id) => id != null));
}

function dedupe(entries, preferredIds) {
  const groups = new Map();
  for (const entry of entries) {
    const key = duplicateKey(entry);
    const group = groups.get(key) || [];
    group.push(entry);
    groups.set(key, group);
  }

  return [...groups.values()].map((group) => {
    group.sort((left, right) => {
      const leftId = fieldValue(left.mapValue?.fields, "id", 0);
      const rightId = fieldValue(right.mapValue?.fields, "id", 0);
      const preferredDelta = Number(preferredIds.has(rightId)) - Number(preferredIds.has(leftId));
      return preferredDelta || leftId - rightId;
    });
    return group[0];
  });
}

async function accessToken() {
  const auth = require(path.join(FIREBASE_TOOLS_ROOT, "lib", "auth"));
  const account = auth.getProjectDefaultAccount(process.cwd()) || auth.getGlobalDefaultAccount();
  if (!account?.tokens?.refresh_token) throw new Error("No Firebase CLI login was found.");
  const tokens = await auth.getAccessToken(account.tokens.refresh_token, []);
  return tokens.access_token;
}

async function request(token, url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}: ${await response.text()}`);
  return response.status === 204 ? null : response.json();
}

async function main() {
  const [uid, mode = "--dry-run"] = process.argv.slice(2);
  if (!uid) throw new Error("Usage: node scripts/repair_cloud_duplicate_gear.js <uid> [--apply]");
  if (!/^[-A-Za-z0-9]{20,128}$/.test(uid)) throw new Error("Invalid Firebase UID.");

  const token = await accessToken();
  const base = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents`;
  const userName = `projects/${PROJECT_ID}/databases/(default)/documents/users/${uid}`;
  const user = await request(token, `${base}/users/${uid}`);
  const original = user.fields?.gearInstances?.arrayValue?.values || [];
  const cleaned = dedupe(original, equippedIds(user));

  console.log(JSON.stringify({
    uid,
    before: original.length,
    after: cleaned.length,
    duplicates: original.length - cleaned.length,
    mode
  }, null, 2));

  if (mode !== "--apply") return;
  if (cleaned.length === original.length) {
    console.log("No exact duplicate gear was found; Firebase was not changed.");
    return;
  }

  const now = Date.now();
  const backupId = `gear-${now}`;
  const backupName = `${userName}/maintenanceBackups/${backupId}`;
  const commitUrl = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents:commit`;
  await request(token, commitUrl, {
    method: "POST",
    body: JSON.stringify({
      writes: [
        {
          update: {
            name: backupName,
            fields: {
              createdAt: { integerValue: String(now) },
              reason: { stringValue: "Exact duplicate gear repair" },
              originalCount: { integerValue: String(original.length) },
              cleanedCount: { integerValue: String(cleaned.length) },
              gearInstances: { arrayValue: { values: original } }
            }
          },
          currentDocument: { exists: false }
        },
        {
          update: {
            name: userName,
            fields: {
              gearInstances: { arrayValue: { values: cleaned } },
              updatedAt: { integerValue: String(now) }
            }
          },
          updateMask: { fieldPaths: ["gearInstances", "updatedAt"] },
          currentDocument: { updateTime: user.updateTime }
        }
      ]
    })
  });

  console.log(`Repair committed. Backup: users/${uid}/maintenanceBackups/${backupId}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
