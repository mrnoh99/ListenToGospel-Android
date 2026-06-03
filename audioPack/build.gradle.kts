plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audioPack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
