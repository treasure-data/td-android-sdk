(function () {
  if (window.TDBridge && window.TDBridge.__ready) { return; }
  var cbs = {}, seq = 0;
  function call(method, args, cb) {
    var id = null;
    if (cb) { id = ++seq; cbs[id] = cb; }
    // Android's @JavascriptInterface accepts strings only, so serialize here.
    __TDBridgeNative.__post(JSON.stringify({ method: method, args: args, callbackId: id }));
  }
  // Native resolves callbacks by evaluating __tdBridgeResolve(id, "<json>").
  window.__tdBridgeResolve = function (id, resultJson) {
    var cb = cbs[id];
    if (cb) {
      delete cbs[id];
      cb(resultJson == null ? null : JSON.parse(resultJson));
    }
  };
  window.TDBridge = {
    __ready: true,
    close:   function ()             { call('close', null, null); },
    openUrl: function (url)          { call('openUrl', { url: url }, null); },
    track:   function (event, values){ call('track', { event: event, values: values || {} }, null); },
    // invoke may resolve a result via the optional callback.
    invoke:  function (name, params, cb) { call('invoke', { name: name, params: params || {} }, cb); }
  };
  // window.TDContext is injected separately by native before this runs.
})();
