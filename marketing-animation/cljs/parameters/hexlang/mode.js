(function(mod) {
  if (typeof exports == "object" && typeof module == "object") // CommonJS
    mod(require("codemirror"));
  else if (typeof define == "function" && define.amd) // AMD
    define(["../../lib/codemirror"], mod);
  else // Plain browser env
    mod(CodeMirror);
})(function(CodeMirror) {
  "use strict";

  CodeMirror.defineMode("hexlang-params", function () {
    return {
      startState: function () {
        return {
          lineNumber: 0,
          inValueSet: false
        };
      },
      token: function (stream, state) {
        if(stream.sol()) {
          state.lineNumber++;
        }

        stream.eatSpace();

        var c;

        // VALUE SET
        if( state.inValueSet ) {
          c = stream.next();
          if( c == "\\" ) {
            stream.next();
            return "value-literal";
          } else if( c == "]" ) {
            state.inValueSet = false;
            return "bracket";
          } else if( c == ",") {
            stream.eatSpace();
            return "";
          } else {
            return "value-literal";
          }
        }
        // PARAMETER
        else {
          c = stream.next();
          if( c == "\\" ) {
            stream.next();
            return "param-literal";
          }
          else if ( c == "[" ) {
            state.inValueSet = true;
            stream.eatSpace();
            return "bracket";
          } else {
            return "param-literal";
          }
        }
      }
    };
  });
});