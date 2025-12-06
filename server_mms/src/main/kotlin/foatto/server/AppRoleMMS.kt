package foatto.server

//--- Don't change or lowercase role values!!! Theis writted in SYSTEM_user.role field
object AppRoleMMS {
    //--- additional (to ADMIN) disabling role for support/managers
    const val SUPPORT: String = "SUPPORT"

    //--- additional (to USER) enabling roles for fixed/static and modile/moveable objects
    const val USER_FIXED_OBJECTS: String = "USER_FIXED_OBJECTS"
    const val USER_MOBILE_OBJECTS: String = "USER_MOBILE_OBJECTS"
}