const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const nodemailer = require("nodemailer");

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "fitnessquestrpg@gmail.com",
    pass: "jfjkxhojjcfmgmgn",
  },
});

exports.onBetaTicketCreated = onDocumentCreated("beta_feedback/{ticketId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  const ticketId = data.ticketId || event.params.ticketId;
  const title = data.title || "No Title";
  const category = data.category || "GENERAL";
  const rating = data.rating || 5;
  const authorHero = data.authorHero || "Pioneer";
  const authorEmail = data.authorEmail || data.email || (data.uid ? `Guest (${data.uid.substring(0, 8)})` : "Guest");
  const comment = data.comment || "";
  const appVersion = data.appVersion || data.appVersionName || "0.13.0";
  const deviceModel = (data.deviceManufacturer && !data.deviceModel?.includes(data.deviceManufacturer))
    ? `${data.deviceManufacturer} ${data.deviceModel}`
    : (data.deviceModel || "Android Device");
  const androidVersion = data.androidVersion || data.androidRelease || "Android";
  const screenshotUrl = data.screenshotUrl || null;

  const subject = `[FitQuest Beta] New Ticket ${ticketId} (OPEN): ${title}`;

  const html = `
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 620px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff; color: #1a202c;">
      <div style="border-bottom: 2px solid #ecc94b; padding-bottom: 12px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
        <h2 style="margin: 0; color: #2d3748; font-size: 20px;">🛡️ Fitness Quest Beta Ticket</h2>
        <span style="background-color: #edf2f7; color: #2d3748; font-weight: bold; font-size: 14px; padding: 4px 10px; border-radius: 6px;">${ticketId}</span>
      </div>

      <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096; width: 32%;">Status</td>
          <td style="padding: 10px 8px; font-weight: bold; color: #38a169;">🟢 OPEN</td>
        </tr>
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096;">Category</td>
          <td style="padding: 10px 8px; color: #2d3748; font-weight: 500;">${category}</td>
        </tr>
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096;">Rating</td>
          <td style="padding: 10px 8px; color: #d69e2e;">${"⭐".repeat(Math.min(5, Math.max(1, rating)))} (${rating}/5)</td>
        </tr>
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096;">Author Hero</td>
          <td style="padding: 10px 8px; color: #2d3748; font-weight: 600;">${authorHero}</td>
        </tr>
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096;">User Account</td>
          <td style="padding: 10px 8px; color: #2d3748;">${authorEmail}</td>
        </tr>
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096;">Summary / Title</td>
          <td style="padding: 10px 8px; color: #2d3748; font-weight: 600;">${title}</td>
        </tr>
        <tr style="border-bottom: 1px solid #edf2f7;">
          <td style="padding: 10px 8px; font-weight: 600; color: #718096;">App & Device</td>
          <td style="padding: 10px 8px; color: #4a5568; font-size: 13px;">FitQuest v${appVersion} • ${deviceModel} (Android ${androidVersion})</td>
        </tr>
      </table>

      <div style="background-color: #f7fafc; border-left: 4px solid #ecc94b; padding: 14px 16px; border-radius: 4px; margin-bottom: 20px;">
        <h4 style="margin: 0 0 8px 0; color: #4a5568; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;">Feedback & Details</h4>
        <p style="margin: 0; color: #1a202c; line-height: 1.6; white-space: pre-wrap; font-size: 15px;">${comment}</p>
      </div>

      ${
        screenshotUrl
          ? `
        <div style="margin-top: 20px; text-align: center; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; background-color: #f7fafc;">
          <h4 style="margin: 0 0 12px 0; color: #4a5568; font-size: 14px;">📸 Attached Screenshot</h4>
          <div style="margin-bottom: 12px;">
            <a href="${screenshotUrl}" target="_blank" style="display: inline-block; padding: 8px 16px; background-color: #3182ce; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 13px;">Open Full Resolution Screenshot</a>
          </div>
          <div>
            <img src="${screenshotUrl}" alt="Ticket Screenshot" style="max-width: 100%; max-height: 450px; border-radius: 6px; border: 1px solid #cbd5e0; object-fit: contain;" />
          </div>
        </div>
      `
          : ""
      }
    </div>
  `;

  try {
    await transporter.sendMail({
      from: '"FitQuest Beta Hub" <fitnessquestrpg@gmail.com>',
      to: "fitnessquestrpg@gmail.com",
      replyTo: (authorEmail.includes("@") && !authorEmail.toLowerCase().includes("fitnessquestrpg@gmail.com")) ? authorEmail : "fitnessquestrpg@gmail.com",
      subject: subject,
      html: html,
    });
    console.log(`Successfully dispatched developer email for ticket ${ticketId}`);
  } catch (error) {
    console.error(`Failed to send developer email for ticket ${ticketId}:`, error);
  }

  // Also send user confirmation receipt if user account has a valid email and is not the dev account
  if (authorEmail.includes("@") && !authorEmail.includes("Guest") && !authorEmail.toLowerCase().includes("fitnessquestrpg@gmail.com")) {
    const userSubject = `[FitQuest Beta] Ticket Received #${ticketId}: ${title}`;
    const userHtml = `
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff; color: #1a202c;">
        <div style="border-bottom: 2px solid #ecc94b; padding-bottom: 12px; margin-bottom: 20px;">
          <h2 style="margin: 0; color: #2d3748; font-size: 20px;">🛡️ Fitness Quest RPG - Feedback Received</h2>
          <p style="margin: 4px 0 0 0; color: #718096; font-size: 14px;">Ticket Number: <strong>${ticketId}</strong></p>
        </div>
        <p>Hi <strong>${authorHero}</strong>,</p>
        <p>Thank you for testing and helping us build Fitness Quest RPG! We have received your submission and added it to our Closed Beta tracker.</p>
        <div style="background-color: #f7fafc; border-left: 4px solid #ecc94b; padding: 12px 16px; border-radius: 4px; margin: 16px 0;">
          <strong>Summary:</strong> ${title}<br/>
          <strong>Category:</strong> ${category}<br/>
          <strong>Details:</strong> ${comment}
        </div>
        <p>You can track updates and developer replies directly in the app under the <strong>Feedback Hub</strong>, or you can <strong>reply directly to this email</strong> if you have any additional information or screenshots to add.</p>
        <p style="color: #718096; font-size: 13px; margin-top: 24px; border-top: 1px solid #edf2f7; padding-top: 12px;">— The Fitness Quest RPG Dev Team</p>
      </div>
    `;

    try {
      await transporter.sendMail({
        from: '"FitQuest Beta Hub" <fitnessquestrpg@gmail.com>',
        to: authorEmail,
        replyTo: "fitnessquestrpg@gmail.com",
        subject: userSubject,
        html: userHtml,
      });
      console.log(`Dispatched user receipt email to ${authorEmail}`);
    } catch (error) {
      console.error(`Failed to send user receipt to ${authorEmail}:`, error);
    }
  }
});

exports.onBetaFollowUpAdded = onDocumentUpdated("beta_feedback/{ticketId}", async (event) => {
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;

  const beforeNotes = beforeData.followUps || [];
  const afterNotes = afterData.followUps || [];

  if (afterNotes.length > beforeNotes.length) {
    const newNote = afterNotes[afterNotes.length - 1];
    const ticketId = afterData.ticketId || event.params.ticketId;
    const authorHero = newNote.authorHero || "Pioneer";
    const noteText = newNote.text || "";

    const subject = `[FitQuest Beta] Update on ${ticketId}: ${authorHero} added notes`;

    const html = `
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff; color: #1a202c;">
        <div style="border-bottom: 2px solid #ecc94b; padding-bottom: 12px; margin-bottom: 20px;">
          <h2 style="margin: 0; color: #2d3748; font-size: 20px;">💬 Ticket Follow-Up Note</h2>
          <p style="margin: 4px 0 0 0; color: #718096; font-size: 14px;">Ticket: <strong>${ticketId}</strong> — ${afterData.title || afterData.category}</p>
        </div>

        <div style="background-color: #f7fafc; border-left: 4px solid #3182ce; padding: 14px 16px; border-radius: 4px; margin-bottom: 20px;">
          <h4 style="margin: 0 0 8px 0; color: #4a5568; font-size: 13px; text-transform: uppercase;">Added by ${authorHero}</h4>
          <p style="margin: 0; color: #1a202c; line-height: 1.6; white-space: pre-wrap; font-size: 15px;">${noteText}</p>
        </div>
      </div>
    `;

    try {
      await transporter.sendMail({
        from: '"FitQuest Beta Hub" <fitnessquestrpg@gmail.com>',
        to: "fitnessquestrpg@gmail.com",
        subject: subject,
        html: html,
      });
      console.log(`Dispatched follow-up email for ${ticketId}`);
    } catch (error) {
      console.error(`Failed to send follow-up email for ${ticketId}:`, error);
    }
  }
});
