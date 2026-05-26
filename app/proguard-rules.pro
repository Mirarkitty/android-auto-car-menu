# Standard androidx.car.app keep rules — the AA host reflects on CarAppService
# subclasses + their session/screen classes.
-keep class com.mirar.carmenu.MyCarAppService { *; }
-keep public class * extends androidx.car.app.CarAppService { *; }
-keep public class * extends androidx.car.app.Session { *; }
-keep public class * extends androidx.car.app.Screen { *; }
