<?php


class ProfileMaker{

public $dir;
public $mCount;
public $version;
public $arr;
public $profile_dat;

public function __construct( $path){
	$this->dir = $path;
	$this->mCount = 0;
	$this->version = 1;	
	$this->arr = array();
	$this->profile_dat = "profile.dat";
}

public function add_profile($name,$content){
	$_arr = array("name"=>$name,"content"=>$content);
	array_push($this->arr,$_arr);	
	
	$this->mCount++;	
}

public function add_file($path){
	$content = file_get_contents($path);
	$name = basename($path,".ovpn");
	$this->add_profile($name,$content);

}

public function add_directory()
{
	$path = $this->dir;
	if(is_dir($path))
	{
		
		$files = array_diff(scandir($path), array('..', '.'));
		foreach($files as $key => $value){
			$f = $path.DIRECTORY_SEPARATOR.$value;
			if(is_file($f))
			{
				$ext = pathinfo($f, PATHINFO_EXTENSION);
				if($ext == "ovpn"){
					$this->add_file($f);
				}
			}
		}
	}else
	{
		echo "is not dir\n";
	}
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

public function writeToFile()
{
			
	$_arr = array("version"=>$this->version,"count"=>$this->mCount,"profiles"=>$this->arr);
	
	$_time = round(microtime(true) * 1000);
	$header = $this->longtobytes($_time);

	$json = json_encode($_arr);
	$gzip_json = gzencode($json);
	$fp = fopen($this->dir."/".$this->profile_dat,"w");
	if($fp){
		fwrite($fp,$header);
		fwrite($fp,$gzip_json);
	}
	fclose($fp);
}

}

/// php profilemaker.php /tmp/ppp/
if($argv && $argv[0] && realpath($argv[0]) === __FILE__) { //equal python __main__
	
	$_dir = $infile=$_SERVER['argv'][1];
	if(!$_dir){
		die("no dir ");
	}
	$tv = new ProfileMaker($_dir);
	$tv->add_directory();
	$tv->writeToFile();

	echo "Write to file ".$tv->dir."/".$tv->profile_dat."\n";
}
