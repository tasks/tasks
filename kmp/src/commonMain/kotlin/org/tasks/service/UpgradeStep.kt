package org.tasks.service

class UpgradeStep(
    val version: Int,
    val upgrade: () -> Upgrade,
)
