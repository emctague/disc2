(function () {
    // Guard against weird duplication
    if (window.hasSetupInjection === true) return;
    window.hasSetupInjection = true;

    // Shorthand for class selection - returns an object with some utilities to manipulate a set of DOM elements.
    // Usage: c`selector goes here`. Surrounding text with curly braces matches class lists that contain that text.
    // Returned object values:
    // - `set(v)` - Applies CSS rules from object 'v' to each element's style object.
    // - `forEach(cb)` - calls NodeList's forEach method on the element list.
    // - `v` - A variable pointing to the first node. Simplifies work with one node only.
    // Returns `null` if no matching elements are found.
    const c = (s, ...k) => {
        let e = document.querySelectorAll(s.raw[0].replace(/{(.+?)}/g, "[class*='$1']"));
        if (!e || e.length == 0) return null;

        return { set: v => e.forEach(o => Object.assign(o.style, v)), forEach: e.forEach, e, v: e[0] };
    };

    // Selectors for various Discord UI components
    const chatSelector = () => c`{content} > {sidebar} + {container}, {chat}`;
    const baseSelector = () => c`{container} {base}`;
    const sidebarSelector = () => c`{guilds}, {sidebar}, {membersWrap}`;

    // SVG Hamburger Icon
    const svgBox = v => `<rect y="${v}" x="0" width="25" height="5" fill="#b9bbbe"/>`;
    const source = `
	<svg x="0" y="0" width="24" height="24" viewBox="0 -5 25 30">
	    ${svgBox(0)}${svgBox(10)}${svgBox(20)}
  	</svg>
  	`;

    const button = document.createElement("div");
    let visible = true;

    // Toggle between visibility
    function toggleView() {
        visible = !visible;

        sidebarSelector().set({ display: visible ? 'flex' : 'none' });
        baseSelector().set({ left: visible ? '72px' : '0px' });

        const chatRect = chatSelector().v.getBoundingClientRect();
        Object.assign(button.style, {
            top: visible ? "0" : "3px",
            right: visible ? "0" : "",
            left: visible ? "" : "3px",
            width: visible ? chatRect.width + "px" : "",
            height: visible ? "100vh" : "",
            backgroundColor: visible ? "rgba(0,0,0,0.7)" : "#35393F",
            position: "fixed",
            padding: "8px",
            zIndex: "1000",
            boxSizing: "border-box"
        });

        button.innerHTML = visible ? '' : source;
    }

    button.addEventListener('click', toggleView);
    document.body.appendChild(button);

    // We want to set up the button once the chat area becomes visible.
    // If we set it up before then, it will not work properly.
    const observer = new MutationObserver(function (mutations) {
        if (mutations.some(m => m.addedNodes) && chatSelector()) {
            observer.disconnect();
            toggleView();
        }
    });

    observer.observe(document.body, {childList: true, subtree: true});
})();
