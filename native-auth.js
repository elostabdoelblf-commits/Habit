(function(){
var cap=/Android/.test(navigator.userAgent)&&location.protocol==='https:'&&location.hostname==='localhost';
if(!cap&&!(window.Capacitor&&Capacitor.isNativePlatform&&Capacitor.isNativePlatform()))return;
var WEB='732643773595-3tnend1459q8pu2l2dg36a0e6rvk1iev.apps.googleusercontent.com',ready=false;
window.AndroidBridge={googleSignIn:function(){
var S=window.Capacitor&&Capacitor.Plugins&&Capacitor.Plugins.SocialLogin;
if(!S){window.hmNativeErr('مكوّن جوجل غير متاح داخل التطبيق');return}
var init=ready?Promise.resolve():S.initialize({google:{webClientId:WEB}}).then(function(){ready=true});
init.then(function(){return S.login({provider:'google',options:{}})}).then(function(r){
var p=(r&&r.result&&r.result.profile)||(r&&r.profile)||{};
window.hmNativeGoogle({n:p.name||p.givenName||p.email||'مستخدم',e:p.email||'',p:p.imageUrl||'',id:p.id||p.email||('g'+Date.now())})
}).catch(function(e){
var m=(e&&(e.message||e.error||e.code))||String(e);
window.hmNativeErr(/cancel|12501|popup_closed/i.test(m)?'تم إلغاء الدخول':'تعذر الدخول بجوجل: '+m)
})}};
})();
