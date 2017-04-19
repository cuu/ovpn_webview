package net.openvpn.openvpn;

public class OpenVPNBase {
    static String app_key;
    static String app_path;
    static String web;

    static boolean download_profile;
    static String profile_link;
    static String author;
    static String about_str;
    static String api_url;
    static String gg_url;
    static String version;
    
    static {
	version="1.3";
	author = "QQ 398437535";
        app_path = "http://softradius.com/app_api/";
        web = "http://softradius.com/";
        app_key = "APP_KEY_CODE";
	
	download_profile = true;
	profile_link = "http://t61.guu.party/profile.dat";
	api_url      = "http://118.178.90.182:5000/u.php";
	gg_url       = "http://118.178.90.182:5000/up.php";
	
	about_str = "关于:<br />"+
"    <a target=\"_blank\" href=\"http://westgalaxy.taobao.com\">官网</a><br />"+
"    QQ : 398437535<br />";
	
    }
}
