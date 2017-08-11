<?php

/*
dalo app的profile.dat的反解
*/
class ProfileDecoder{

public $profile_dat;
public $offset;
public $line_dir;

public function __construct( $path,$offset){
	$this->profile_dat = $path;
	$this->offset = $offset;
	$this->line_dir = "lines";

}

private function pack_array($format, $arg)
{
    $result="";
    foreach ($arg as $item) $result .= pack ($format, $item);
    return $result;
}

public function longtobytes($n)
{
	$res = array();
	$j=0;
	$ll = 8;
	for($i=0;$i<$ll;$i++){
		array_push($res, ($n >> $j) & 0xff);
		$j+=8;
	}	
	$rev = array_reverse($res);
	return $this->pack_array("C",$rev);	
}

public function loadFromfile()
{
	$fp = fopen($this->profile_dat,"r");
	if($fp){
		fseek($fp,$this->offset);
		$cnt = "";
		while(!feof($fp)){
			$cnt .= fread($fp,8192);
		}
		$cnt = gzdecode($cnt);
	}
	fclose($fp);
	return $cnt;
}

}


$OFFSET=8;
$options = getopt("hd:vf:");

if($argv && $argv[0] && realpath($argv[0]) === __FILE__) { //equal python __main__

	if(!isset($options["f"])){
		die("no profile.dat");
	}

	$_file = $options["f"];
	$tt = new ProfileDecoder( $_file,$OFFSET);
	$data = json_decode( $tt->loadFromfile(),true) ;

	if(isset($options["v"])){
		foreach ($data["profiles"] as $value){
			echo $value["name"]."\n";
			echo $value["content"]."\n";
		}
	}

	if(isset($options["d"])){
		mkdir($options["d"]);
		foreach($data["profiles"] as $value){
		     echo $options["d"]."/".$value["name"].".ovpn\n";
		     file_put_contents($options["d"]."/".$value["name"].".ovpn", $value["content"]);
		}
	}
	
	if(isset($options["h"])){
		echo "php me -d outputdir -f profile.dat\n";
	}
}
