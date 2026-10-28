(function(){
var cap=/Android/.test(navigator.userAgent)&&location.protocol==='https:'&&location.hostname==='localhost';
if(!cap&&!(window.Capacitor&&Capacitor.isNativePlatform&&Capacitor.isNativePlatform()))return;
window.AndroidBridge={googleSignIn:function(){
var G=window.Capacitor&&Capacitor.Plugins&&Capacitor.Plugins.GoogleAuth;
if(!G){window.hmNativeErr&&hmNativeErr('مكوّن جوجل غير متاح داخل التطبيق');return}
G.signIn().then(function(r){
window.hmNativeGoogle({n:r.name||r.givenName||r.email||'مستخدم',e:r.email||'',p:r.imageUrl||'',id:r.id})
}).catch(function(e){
var m=(e&&(e.message||e.error||e.code))||String(e);
window.hmNativeErr(/cancel|12501|popup_closed/i.test(m)?'تم إلغاء الدخول':'تعذر الدخول بجوجل: '+m)
})}};
})();
