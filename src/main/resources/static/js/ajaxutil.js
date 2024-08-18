// 登录之后访问方法
function  authAjax(jquery,url,data,callback){
    jquery.ajax({
        url: url,
        type: "POST",
        headers: {
            'Authrization': layui.data('login_user_info_key').authrization
        },
        data: data,
        success: callback
    });
}