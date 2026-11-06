package com.digitalvault.ui

/** Tamam UI text ek jagah. Part 7 mein Urdu/English language selection isi par lagega. */
object S {
    const val appName = "Digital Vault"
    const val tagline = "Your private, offline password vault"

    const val masterTitle = "Create your Master Password"
    const val masterHint = "At least 10 characters, using 3 of these: capital letters, small letters, numbers, symbols. It protects all your data."
    const val masterLabel = "Master Password"
    const val confirmLabel = "Confirm password"
    const val mismatch = "Passwords don't match."
    const val weakMaster = "That Master Password is too weak."
    const val next = "Next"
    const val back = "Back"

    const val pinTitle = "Set a PIN"
    const val pinHint = "4 to 6 digits for quick unlock. Your data is protected by the Master Password, not by the PIN alone."
    const val pinLabel = "PIN"
    const val pinConfirm = "Confirm PIN"

    const val recoveryTitle = "Your Recovery Key"
    const val recoveryHint = "Write this key on paper and keep it safe. If you forget your Master Password, it's the only way to get your data back. It won't be shown again."
    const val recoveryConfirm = "I've written down my Recovery Key"
    const val createVault = "Create vault"
    const val setupFailed = "Couldn't create the vault. Try again."

    const val enterPin = "Enter your PIN"
    const val enterMaster = "Enter your Master Password"
    const val unlock = "Unlock"
    const val useMaster = "Use Master Password"
    const val usePin = "Use PIN"
    const val forgotPin = "Forgot PIN?"
    const val forgotMaster = "Forgot Master Password?"
    const val fingerprint = "Fingerprint"
    const val delete = "Delete"

    const val recoveryEnterTitle = "Reset with Recovery Key"
    const val recoveryCodeLabel = "Recovery Key"
    const val newMasterLabel = "New Master Password"
    const val resetUnlock = "Reset and unlock"

    const val newPinTitle = "Set a new PIN"
    const val savePin = "Save PIN"

    const val fingerprintOfferTitle = "Unlock with fingerprint?"
    const val fingerprintOfferBody = "You can open the vault with your fingerprint. You can change this later in Settings."
    const val enable = "Enable"
    const val notNow = "Not now"
    const val cancel = "Cancel"
    const val fingerprintUnlockTitle = "Unlock Digital Vault"
    const val fingerprintEnrollTitle = "Enable fingerprint"

    const val unlocked = "Vault unlocked"
    const val unlockedBody = "The real screens arrive in Part 4."
    const val lockNow = "Lock now"

    fun wrong(left: Int) = "Wrong details. $left attempt(s) left before a temporary lock."
    fun locked(time: String) = "Too many wrong attempts. Try again in $time."
    const val failedRecovery = "That Recovery Key didn't work."
}
