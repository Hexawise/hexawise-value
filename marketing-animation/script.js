function mountAnimationGraphics() {
	const badTestData = [
		[2, 2, 2, 2, 2, 2, 2, 2],
		[2, 1, 2, 2, 2, 2, 2, 2],
		[2, 2, 2, 3, 2, 2, 2, 2],
		[2, 2, 2, 2, 2, 2, 2, 1],
		[3, 2, 2, 2, 2, 2, 2, 2],
		[2, 2, 2, 2, 1, 2, 2, 2],
		[2, 2, 2, 2, 2, 2, 3, 2],
		[3, 2, 2, 2, 2, 2, 2, 2],
		[1, 2, 2, 2, 2, 2, 2, 3],
		[2, 2, 2, 1, 3, 2, 2, 2],
		[2, 2, 2, 2, 2, 1, 2, 2],
		[2, 2, 2, 2, 2, 3, 2, 2],
		[2, 2, 3, 2, 2, 2, 2, 2],
		[2, 2, 1, 2, 2, 2, 2, 2],
		[2, 2, 2, 2, 2, 2, 1, 2],
		[2, 2, 1, 2, 2, 2, 3, 2],
		[2, 3, 2, 2, 1, 2, 2, 2]
	];

	const goodTestData = [
		[1, 1, 1, 1, 1, 1, 1, 1],
		[2, 2, 2, 2, 2, 2, 1, 2],
		[3, 3, 3, 3, 3, 3, 1, 3],
		[1, 3, 2, 1, 3, 2, 2, 1],
		[1, 2, 3, 1, 2, 3, 3, 1],
		[2, 1, 3, 3, 1, 1, 2, 2],
		[3, 3, 1, 2, 3, 1, 3, 2],
		[3, 1, 2, 3, 1, 2, 3, 3],
		[3, 2, 1 ,2, 2, 1, 2, 3],
		[1, 1, 1, 2, 1, 3, 2, 2],
		[2, 1, 3, 2, 2, 2, 3, 1],
		[1, 2, 1, 3, 2, 2, 3, 1],
		[2, 1, 2, 1, 3, 1, 1, 2],
		[2, 2, 2, 1, 1, 3, 1, 3],
		[2, 3, 1, 2, 1, 1, 3, 1],
		[3, 3, 1, 1, 2, 1, 2, 1],
		[1, 2, 2, 1, 3, 3, 1, 3]
	];

  const urlParams = new URLSearchParams(window.location.search);
  const encodedLabels = urlParams.get('p');
	const labels = JSON.parse(atob(encodedLabels));

	const badColor = "#777";
	const goodColor = "#7258c0";
	const nodeColor = "#444";

	const width = 940;
	const height = 600;
	const linkOuterWidth = 8;
	const linkInnerWidth = 6;
	const nodeOuterWidth = 12;
	const nodeInnerWidth = 4;

  // Render bad coverage matrix
  const stepBadCoverageMatrix = coverage_matrix.core.start_BANG_({
    dom: "bad-test-matrix",
    data: badTestData,
    coverage: [28,7,7,7,7,7,7,7,7,7,7,7,7,7,7,4,3],
    labels
  });

  // Render good coverage matrix
  const stepGoodCoverageMatrix = coverage_matrix.core.start_BANG_({
    dom: "good-test-matrix",
    data: goodTestData,
    coverage: [28,28,28,23,23,23,22,20,17,10,6,6,5,5,3,3,2],
    labels
  });

  // Render bad test web
	const stepBadTestWeb = renderParallelCoordinates({
		containerId: "bad-test",
		data: badTestData,
		linkColor: badColor,
		nodeColor,
		width,
		height,
		linkOuterWidth,
		linkInnerWidth,
		nodeOuterWidth,
		nodeInnerWidth,
	});

  // Render good test web
	const stepGoodTestWeb = renderParallelCoordinates({
		containerId: "good-test",
		data: goodTestData,
		width,
		height,
		linkColor: goodColor,
		nodeColor,
		width,
		height,
		linkOuterWidth,
		linkInnerWidth,
		nodeOuterWidth,
		nodeInnerWidth,
	});

  // Return handles allowing consumer to "step" animations forward test-by-test
  return {
    stepBadTestWeb,
    stepBadCoverageMatrix,
    stepGoodTestWeb,
    stepGoodCoverageMatrix,
  };




  /*
   * PRIVATE FUNCTION DEFINITIONS BELOW THIS POINT
   */

  // Chart
	function renderParallelCoordinates(options) {
		const containerId = options.containerId;
		const data = options.data;
		const svgWidth = options.width;
		const svgHeight = options.height;
		const nodeColor = options.nodeColor;
		const linkColor = options.linkColor;
		const linkOuterWidth = options.linkOuterWidth;
		const linkInnerWidth = options.linkInnerWidth;
		const nodeOuterWidth = options.nodeOuterWidth;
		const nodeInnerWidth = options.nodeInnerWidth;

		// Set up
    // NOTE: Also look at the "styles.css" file for other dimension properties
		const margin = { top: 80, right: 50, bottom: 150, left: 60 };
		const width = svgWidth - margin.left - margin.right;
		const height = svgHeight - margin.top - margin.bottom;

		const numTests = data.length;
		const allCols = d3.range(data[0].length);
		const allRows = Array.from(new Set(flat(data)).values()).sort(
			(a, b) => a - b
		);

		const x = d3
			.scalePoint()
			.domain(allCols)
			.range([0, width]);
		const y = d3
			.scalePoint()
			.domain(allRows)
			.range([0, height]);

		// Process data
		const nodesMap = new Map();
		const nodes = d3.cross(allCols, allRows).map(d => {
			const i = d[0];
			const j = d[1];
			const id = nodeId(i, j);
			const node = {
				id,
				i,
				j,
				x: x(i),
				y: y(j),
				ins: Array(numTests).fill([]),
				outs: Array(numTests).fill([])
			};
			nodesMap.set(id, node);
			return node;
		});

		const links = data.map((d, k) => {
			const segments = [];
			if (k > 0) {
				nodes.forEach(d => {
					d.outs[k] = d.outs[k - 1].slice();
					d.ins[k] = d.ins[k - 1].slice();
				});
			}
			for (let i = 0; i < d.length - 1; i++) {
				const source = nodesMap.get(nodeId(i, d[i]));
				const target = nodesMap.get(nodeId(i + 1, d[i + 1]));
				const id = linkId(k, source, target);
				const segment = {
					id,
					source,
					target
				};
				segments.push(segment);
				source.outs[k].push(id);
				target.ins[k].push(id);
			}
			return segments;
		});

		const line = d3.line().curve(d3.curveMonotoneX);

		const segmentPathGenerator = d => {
			const offset = 0.35 * x.step();
			const sourceIndex = d.source.outs[k].indexOf(d.id);
			const targetIndex = d.target.ins[k].indexOf(d.id);

			const x0 = d.source.x;
			const y0 = d.source.y + d.source.y1 + sourceIndex * linkOuterWidth;
			const x1 = d.source.x + offset;
			const y1 = y0;
			const x2 = d.target.x - offset;
			const y2 = d.target.y + d.target.y1 + targetIndex * linkOuterWidth;
			const x3 = d.target.x;
			const y3 = y2;

			return line([[x0, y0], [x1, y1], [x2, y2], [x3, y3]]);
		};

		// Static layout
		const svg = d3
			.select(`#${containerId}`)
			.append("svg")
			.attr("width", svgWidth)
			.attr("height", svgHeight);

		const g = svg
			.append("g")
			.attr("transform", `translate(${margin.left},${margin.top})`);

		// Measure single text line height
		let lineHeight;
		g.append("text")
			.text("ABCDEFGHIJKLMNOPQRESTUVWXYZabcdefghijklmnopqrstuvwxyz")
			.each(function() {
				lineHeight = this.getBBox().height;
			})
			.remove();

		g.append("g")
			.selectAll("g")
			.data(labels)
			.join("g")
			.attr("transform", (d, i) => `translate(${x(i)}, -40)`)
			.append("text")
			.attr("class", "column-label")
			.attr("text-anchor", "middle")
			.attr("font-weight", "bold")
			.attr("dy", 0)
			.text(d => d.col)
			.call(wrap, x.step() * 1.0)
			.each(function() {
				const height = this.getBBox().height;
				d3.select(this).attr(
					"transform",
					`translate(0,${-height + lineHeight})`
				);
			});

		const tests = g
			.selectAll(".tests-g")
			.data(links)
			.join("g")
			.attr("class", "tests-g");

		const grid = g
			.selectAll(".grid-g")
			.data(nodes)
			.join("g")
			.attr("class", "grid-g")
			.attr("transform", d => `translate(${d.x},${d.y})`);

		grid
			.append("text")
			.attr("class", "grid-label")
			.attr("text-anchor", "middle")
			.attr("y", (linkOuterWidth * numTests) / 2.25 + lineHeight)
			.text(d => labels[d.i].label[d.j]);

		grid
			.append("line")
			.attr("class", "node-line node-line--outer")
			.attr("stroke-linecap", "round")
			.attr("stroke", nodeColor)
			.attr("stroke-width", nodeOuterWidth)
			.clone(true)
			.attr("class", "node-line node-line--inner")
			.attr("stroke", "#fff")
			.attr("stroke-width", nodeInnerWidth);

		let k = 0;

		function animateTest(duration) {

		  // Animate
		  const durationRearrange = duration * 0.3;
		  const durationSegment = (duration - durationRearrange) / links[0].length;

			// Update data
			nodes.forEach(d => {
				const count = Math.max(d.ins[k].length, d.outs[k].length);
				if (count === 0) {
					d.y1 = 0;
					d.y2 = 0;
				} else if (count % 2) {
					d.y1 = (-(count - 1) / 2) * linkOuterWidth;
					d.y2 = ((count - 1) / 2) * linkOuterWidth;
				} else {
					d.y1 = (-count / 2) * linkOuterWidth;
					d.y2 = ((count - 2) / 2) * linkOuterWidth;
				}
			});

			const testData = links[k];

			const prevTests = tests.selectAll(".segment");
			const newTest = tests.append("g").attr("class", "test-g");

			newTest
				.append("text")
				.datum(testData[0].source)
				.attr("class", "test-label")
				.style("fill", linkColor)
				.attr("x", d => d.x)
				.attr("y", d => d.y)
				.attr("dx", -20)
				.attr("dy", "0.35em")
				.attr("text-anchor", "end")
				.text(`Test ${k+1}`);

			const tRearrange = svg.transition().duration(durationRearrange);

			grid
				.selectAll(".node-line");
				.transition(tRearrange);
				.attr("y1", d => d.y1);
				.attr("y2", d => d.y2);

			prevTests
				.attr("stroke-dasharray", null)
				.transition(tRearrange)
				.attr("d", segmentPathGenerator);

			tRearrange.end().then(() => {
				let segmentIndex = 0;

				function animateSegment() {
					newTest
						.append("path")
						.datum(testData[segmentIndex])
						.attr("class", "segment")
						.attr("stroke", linkColor)
						.attr("stroke-width", linkInnerWidth)
						.attr("stroke-linejoin", "round")
						.attr("fill", "none")
						.attr("d", segmentPathGenerator)
						.each(function(d) {
							d.totalLength = this.getTotalLength();
						})
						.attr("stroke-dasharray", d => d.totalLength)
						.attr("stroke-dashoffset", d => d.totalLength)
						.transition()
						.duration(durationSegment)
						.ease(d3.easeLinear)
						.attr("stroke-dashoffset", 0)
						.end()
						.then(() => {
							segmentIndex++;
							if (segmentIndex < testData.length) {
								animateSegment();
							} else {
								newTest.select(".test-label").remove();
								k++;
								// if (k < numTests) animateTest();
							}
						});
				}
				animateSegment();
			});
		}

    // Return function that allows outside consumer to "step" this animation forward
    return animateTest;
	}

  // Utilities
	function nodeId(i, j) {
		return `n-${i}-${j}`;
	}

	function linkId(k, source, target) {
		return `l-${k}-${source.id}-${target.id}`;
	}

	function flat(arr) {
		return [].concat(...arr);
	}

	function wrap(text, width) {
		text.each(function() {
			var text = d3.select(this),
				words = text
					.text()
					.split(/\s+/)
					.reverse(),
				word,
				line = [],
				lineNumber = 0,
				lineHeight = 1, // ems
				y = text.attr("y"),
				dy = parseFloat(text.attr("dy")),
				tspan = text
					.text(null)
					.append("tspan")
					.attr("x", 0)
					.attr("y", y)
					.attr("dy", dy + "em");
			while ((word = words.pop())) {
				line.push(word);
				tspan.text(line.join(" "));
				if (tspan.node().getComputedTextLength() > width) {
					line.pop();
					tspan.text(line.join(" "));
					line = [word];
					tspan = text
						.append("tspan")
						.attr("x", 0)
						.attr("y", y)
						.attr("dy", ++lineNumber * lineHeight + dy + "em")
						.text(word);
				}
			}
		});
	}
}
