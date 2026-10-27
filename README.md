# HABIT OS — بناء ملف APK

## الطريقة الأسهل (بدون تثبيت أي شيء) — GitHub Actions
1. اعمل حساب على github.com وأنشئ Repository جديد (Private أو Public).
2. ارفع كل محتويات هذا المجلد (بما فيها مجلد .github).
3. ادخل تبويب Actions ← Build APK ← Run workflow.
4. بعد ~5 دقائق نزّل الملف من Artifacts: HABIT-OS-apk (app-debug.apk) وثبّته على الموبايل.

## الطريقة المحلية (Android Studio)
```
npm install
npx cap add android
npx capacitor-assets generate --android --assetPath assets
npx cap sync android
npx cap open android   # Build > Build APK(s)
```

## للنشر على Google Play
ملف debug للتجربة والتثبيت المباشر. للنشر لازم Release موقّع (AAB): Build > Generate Signed Bundle في Android Studio.
