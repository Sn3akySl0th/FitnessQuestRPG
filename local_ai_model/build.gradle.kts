plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("local_ai_model")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
