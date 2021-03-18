/*添加窗口加载方法*/
function addWindowLoadEvent(func) {
	var oldonload = window.onload;
	if(typeof window.onload != "function") {
		window.onload = func;
	} else {
		window.onload = function() {
			oldonload();
			func();
		}
	}
}
function init(){
    //启动背景动画
    can = $("#bg-canvas");
    can.space_animation({
        window_width: can[0].clientWidth,
        window_height: can[0].clientHeight,
        window_background: '#000810',
        star_count: '1000',
        star_color: can[0].dataset.fgColor,
        star_depth: '500'
    });
}
addWindowLoadEvent(init);