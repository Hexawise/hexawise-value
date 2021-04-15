(function() {

  // A handle to the SVG graphics on the screen. Set by the "mountGraphics()" function.
  var anim;

  // The number of milliseconds that it takes for a single test to animate from
  // start to finish
  const testDuration = 600;
  const delayBetweenTests = testDuration + 200;

  const badChartTitle = document.getElementById("bad-chart-title");
  const badTestWeb = document.getElementById("bad-test");
  const badMatrix = document.getElementById("bad-test-matrix");
  const becomeWasteful = document.querySelector("#become-wastefully-repetitive .chart-title");
  const becomeWastefulArrow = document.getElementById("become-wasteful-arrow");
  const andLeaveGaps = document.querySelector("#leave-gaps .chart-title");
  const leaveGapsArrow = document.getElementById("leave-gaps-arrow");

  const goodChartTitle = document.getElementById("good-chart-title");
  const goodTestWeb = document.getElementById("good-test");
  const goodMatrix = document.getElementById("good-test-matrix");
  const thoroughlyCovering = document.querySelector("#thoroughly-covering .chart-title");
  const thoroughlyCoveringArrow = document.getElementById("thoroughly-covering-arrow");

  /*
   * Define the animation timeline as an array of keyframes, each keyframe a tuple of:
   *
   *  [delay, functionToCall]
   *
   * where "delay" is the number of milliseconds since the previous keyframe fired
   * to wait before calling "functionToCall".
   */
  const timeline = [
    // Bad tests sequence
    [2000,              () => fadeIn(badChartTitle)],
    [1500,              () => fadeIn(becomeWasteful)],
    [500,               () => fadeIn(becomeWastefulArrow)],
    [500,               () => anim = mountAnimationGraphics()],
    [500,               () => fadeIn(badTestWeb)],
    [500,               () => fadeIn(badMatrix)],
    [500,               stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [0,                 () => { partialFadeOut(becomeWasteful); fadeOut(becomeWastefulArrow); }],
    [0,                 () => fadeIn(andLeaveGaps)],
    [200,               () => fadeIn(leaveGapsArrow)],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],
    [delayBetweenTests, stepBad],

    // Good tests sequence
    [2000,              () => {partialFadeOut(andLeaveGaps); fadeOut(leaveGapsArrow);}],
    [0,                 () => fadeIn(goodChartTitle)],
    [2000,              () => fadeIn(goodTestWeb)],
    [500,               () => fadeIn(goodMatrix)],
    [100,               stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [0,                 () => fadeIn(thoroughlyCovering)],
    [200,               () => fadeIn(thoroughlyCoveringArrow)],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [delayBetweenTests, stepGood],
    [1000,              () => {partialFadeOut(thoroughlyCovering); fadeOut(thoroughlyCoveringArrow);}],
  ];

  // Schedule the timeline for execution
  let runningTime = 0;
  for(var i = 0; i < timeline.length; i++) {
    let lastKeyframe = (i > 0) ? timeline[i - 1] : [0];
    runningTime += lastKeyframe[0];
    let keyframe = timeline[i];
    let duration = runningTime + keyframe[0];
    let fn = keyframe[1];
    setTimeout(fn, duration);
  }


  /*
   * PRIVATE FUNCTIONS BEYOND THIS POINT
   */

  /*
   * BAD ANIMATIONS
   */

  function stepBad() {
    anim.stepBadTestWeb(testDuration);
    setTimeout(() => anim.stepBadCoverageMatrix(), testDuration);
  }

  function fadeIn(element) {
    element.style.opacity = 1;
  }

  function partialFadeOut(element) {
    element.style.opacity = 0.5;
  }

  function fadeOut(element) {
    element.style.opacity = 0;
  }

  /*
   * GOOD ANIMATIONS
   */

  function stepGood() {
    anim.stepGoodTestWeb(testDuration);
    setTimeout(() => anim.stepGoodCoverageMatrix(), testDuration);
  }


  function animateGoodChartTitle() {
    const goodChartTitle = document.getElementById("good-chart-title");
    goodChartTitle.style.opacity = 1;
  }
})();