/*
 * Copyright (C) 2013 www.418log.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ab.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.ab.global.AbAppData;
import com.ab.global.AbConstant;

// TODO: Auto-generated Javadoc
/**
 * 描述：文件操作类.
 *
 * @author zhaoqp
 * @date 2011-12-10
 * @version v1.0
 */
public class AbFileUtil {
	
	/** The tag. */
	private static String TAG = "AbFileUtil";
	
	/** The Constant D. */
	private static final boolean D = AbAppData.DEBUG;
	
	/** 默认下载文件地址. */
	private static  String downPathRootDir = File.separator + "download" + File.separator;
	
    /** 默认下载图片文件地址. */
	private static  String downPathImageDir = downPathRootDir + "cache_images" + File.separator;
    
    /** 默认下载文件地址. */
	private static  String downPathFileDir = downPathRootDir + "cache_files" + File.separator;
    
	/**MB  单位B*/
	private static int MB = 1024*1024;
	
	/**缓存文件夹的大小100M  单位B*/
	private static int cacheSize = 100*MB;
	
    /**剩余空间大于200M才使用缓存*/
	private static int freeSdSpaceNeededToCache = 200*MB;
	
	/**缓存空间当前的大小，临时*/
	private static int dirSize = -1;
	
	/**统计程序下载的图片数，超过10张检查sd卡*/
	private static int downCount = 0;
	/**
	 * 下载网络文件到SD卡中.如果SD中存在同名文件将不再下载
	 * @param url 要下载文件的网络地址
	 * @return 下载好的本地文件地址
	 */
	 public static String downFileToSD(String url,String name){
		 InputStream in = null;
		 FileOutputStream fileOutputStream = null;
		 HttpURLConnection con = null;
		 String downFilePath = null;
		 File file = null;
		 try {
	    	if(!isCanUseSD()){
	    		return null;
	    	}
	    	File path = Environment.getExternalStorageDirectory();
	    	File fileDirectory = new File(path.getAbsolutePath() + downPathImageDir);
			if(!fileDirectory.exists()){
				fileDirectory.mkdirs();
			}
			
			file = new File(fileDirectory,name);
			if(!file.exists()){
				file.createNewFile();
			}else{
				//文件已经存在
				if(file.length()!=0){
					return file.getPath();
				}
			}
			downFilePath = file.getPath();
			URL mUrl = new URL(url);
			con = (HttpURLConnection)mUrl.openConnection();
			con.connect();
			in = con.getInputStream();
			fileOutputStream = new FileOutputStream(file);
			byte[] b = new byte[1024];
			int temp = 0;
			while((temp=in.read(b))!=-1){
				fileOutputStream.write(b, 0, temp);
			}
		}catch(Exception e){
			if(D)Log.d(TAG, ""+e.getMessage());
			return null;
		}finally{
			try {
				if(in!=null){
					in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if(fileOutputStream!=null){
					fileOutputStream.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if(con!=null){
					con.disconnect();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				//检查文件大小,如果文件为0B说明网络不好没有下载成功，要将建立的空文件删除
				if(file.length() == 0){
					file.delete();
				}else{
					downCount ++;
				}
				if(downCount >= 10){
					//检查空间将很久未使用的文件删除
					removeCache();
					downCount = 0;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return downFilePath;
	 }
	 
	 /**
	  * 描述：通过文件的网络地址从SD卡中读取图片，如果SD中没有则自动下载并保存.
	  * @param url 文件的网络地址
	  * @param type 图片的处理类型（剪切或者缩放到指定大小，参考AbConstant类）
	  * 如果设置为原图，则后边参数无效，得到原图
	  * @param newWidth 新图片的宽
	  * @param newHeight 新图片的高
	  * @return Bitmap 新图片
	  */
	 public static Bitmap getBitmapFromSDCache(String url,int type,int newWidth, int newHeight){
		 Bitmap bit = null;
		 try {
			 if(AbStrUtil.isEmpty(url)){
		    	return null;
		     }
			 
			 //SD卡不存在 或者剩余空间不足了就不缓存到SD卡了
			 if(!isCanUseSD() || freeSdSpaceNeededToCache < freeSpaceOnSD()){
				 bit = getBitmapFormURL(url,type,newWidth,newHeight);
				 return bit;
		     }
			 
			 if(type != AbConstant.ORIGINALIMG && ( newWidth<=0 || newHeight<=0)){
				 throw new IllegalArgumentException("缩放和裁剪图片的宽高设置不能小于0");
			 }
			 
			 //文件是否存在
			 File path = Environment.getExternalStorageDirectory();
			 File fileDirectory = new File(path.getAbsolutePath() + downPathImageDir);
			 
			 //获取后缀
			 String suffix = getSuffixFromUrl(url);
			 
			 //缓存的图片文件名的组合
			 String fileName = getImageFileName(url,newWidth,newHeight,type);
			 File file = new File(fileDirectory,fileName+suffix);
			 if(!file.exists()){
				 downFileToSD(url,file.getName());
				 return getBitmapFromSD(file,type,newWidth,newHeight);
			 }else{
				 //文件存在
				 if(type == AbConstant.CUTIMG){
			 		bit = AbImageUtil.cutImg(file,newWidth,newHeight);
			 	 }else if(type == AbConstant.SCALEIMG){
			 		bit = AbImageUtil.scaleImg(file,newWidth,newHeight);
			 	 }else{
			 		bit = AbImageUtil.originalImg(file);
			 	 }
			 	 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bit;
		
	 }
	 
	 /**
	  * 描述：通过文件的网络地址从SD卡中读取图片.
	  * @param url 文件的网络地址
	  * @param type 图片的处理类型（剪切或者缩放到指定大小，参考AbConstant类）
	  * 如果设置为原图，则后边参数无效，得到原图
	  * @param newWidth 新图片的宽
	  * @param newHeight 新图片的高
	  * @return Bitmap 新图片
	  */
	 public static Bitmap getBitmapFromSD(String url,int type,int newWidth, int newHeight){
		 Bitmap bit = null;
		 try {
			 //SD卡是否存在
			 if(!isCanUseSD()){
				 return null;
		     }
			 
			 if(type != AbConstant.ORIGINALIMG && ( newWidth<=0 || newHeight<=0)){
				 throw new IllegalArgumentException("缩放和裁剪图片的宽高设置不能小于0");
			 }
			 
			 //文件是否存在
			 File path = Environment.getExternalStorageDirectory();
			 File fileDirectory = new File(path.getAbsolutePath() + downPathImageDir);
			 String fileName = getImageFileName(url,newWidth,newHeight,type);
			 File file = new File(fileDirectory,fileName);
			 if(!file.exists()){
				 return null;
			 }else{
				 //文件存在
				 if(type == AbConstant.CUTIMG){
			 		bit = AbImageUtil.cutImg(file,newWidth,newHeight);
			 	 }else if(type == AbConstant.SCALEIMG){
				 	bit = AbImageUtil.scaleImg(file,newWidth,newHeight);
			 	 }else{
			 		bit = AbImageUtil.originalImg(file);
			 	 }
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bit;
		
	 }
	 
	 /**
 	 * 描述：通过文件的本地地址从SD卡读取图片.
 	 *
 	 * @param file the file
 	 * @param type 图片的处理类型（剪切或者缩放到指定大小，参考AbConstant类）
 	 * 如果设置为原图，则后边参数无效，得到原图
 	 * @param newWidth 新图片的宽
 	 * @param newHeight 新图片的高
 	 * @return Bitmap 新图片
 	 */
	 public static Bitmap getBitmapFromSD(File file,int type,int newWidth, int newHeight){
		 Bitmap bit = null;
		 try {
			 //SD卡是否存在
			 if(!isCanUseSD()){
		    	return null;
		     }
			 
			 if(type != AbConstant.ORIGINALIMG && ( newWidth<=0 || newHeight<=0)){
				 throw new IllegalArgumentException("缩放和裁剪图片的宽高设置不能小于0");
			 }
			 
			 //文件是否存在
			 if(!file.exists()){
				 return null;
			 }
			 //文件存在
			 if(type==AbConstant.CUTIMG){
		 		bit = AbImageUtil.cutImg(file,newWidth,newHeight);
		 	 }else if(type == AbConstant.SCALEIMG){
			 	bit = AbImageUtil.scaleImg(file,newWidth,newHeight);
		 	 }else{
		 		bit = AbImageUtil.originalImg(file);
		 	 }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bit;
	 }
	 
	 /**
 	 * 描述：通过文件的本地地址从SD卡读取图片.
 	 *
 	 * @param file the file
 	 * @return Bitmap 图片
 	 */
	 public static Bitmap getBitmapFromSD(File file){
		 Bitmap bitmap = null;
		 try {
			 //SD卡是否存在
			 if(!isCanUseSD()){
		    	return null;
		     }
			 //文件是否存在
			 if(!file.exists()){
				 return null;
			 }
			 //文件存在
			 bitmap = AbImageUtil.originalImg(file);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	 }
	 
	 /**
	  * 描述：将图片的byte[]写入本地文件.
	  * @param imgByte 图片的byte[]形势
	  * @param fileName 文件名称，需要包含后缀，如.jpg
	  * @param type 图片的处理类型（剪切或者缩放到指定大小，参考AbConstant类）
	  * @param newWidth 新图片的宽
	  * @param newHeight 新图片的高
	  * @return Bitmap 新图片
	  */
     public static Bitmap getBitmapFormByte(byte[] imgByte,String fileName,int type,int newWidth, int newHeight){
    	   FileOutputStream fos = null;
    	   DataInputStream dis = null;
    	   ByteArrayInputStream bis = null;
    	   Bitmap b = null;
    	   File file = null;
    	   try {
    		   if(imgByte!=null){
    			   File sdcardDir = Environment.getExternalStorageDirectory();
    			   String path = sdcardDir.getAbsolutePath()+downPathImageDir;
    			   file = new File(path+fileName);
    				 
    			   if(!file.getParentFile().exists()){
    			          file.getParentFile().mkdirs();
    			   }
    			   if(!file.exists()){
    			          file.createNewFile();
    			   }
    			   fos = new FileOutputStream(file);
    			   int readLength = 0;
    			   bis = new ByteArrayInputStream(imgByte);
    			   dis = new DataInputStream(bis);
    			   byte[] buffer = new byte[1024];
    			   
    			   while ((readLength = dis.read(buffer))!=-1) {
    				   fos.write(buffer, 0, readLength);
    			       try {
    						Thread.sleep(500);
    				   } catch (Exception e) {
    				   }
    			   }
    			   fos.flush();
    			   
    			   b = getBitmapFromSD(file,type,newWidth,newHeight);
    		   }
			   
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(dis!=null){
				try {
					dis.close();
				} catch (Exception e) {
				}
			}    
			if(bis!=null){
				try {
					bis.close();
				} catch (Exception e) {
				}
			}
			if(fos!=null){
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
		}
        return  b;
     }
	    
	/**
	 * 描述：根据URL从互连网获取图片.
	 * @param url 要下载文件的网络地址
	 * @param type 图片的处理类型（剪切或者缩放到指定大小，参考AbConstant类）
	 * @param newWidth 新图片的宽
	 * @param newHeight 新图片的高
	 * @return Bitmap 新图片
	 */
	public static Bitmap getBitmapFormURL(String url,int type,int newWidth, int newHeight){
		Bitmap bit = null;
		try {
			bit = AbImageUtil.getBitmapFormURL(url, type, newWidth, newHeight);
	    } catch (Exception e) {
	    	 if(D)Log.d(TAG, "下载图片异常："+e.getMessage());
		}
	    if(D)Log.d(TAG, "返回的Bitmap："+bit);
		return bit;
	}
	
	/**
	 * 描述：获取src中的图片资源.
	 *
	 * @param src 图片的src路径，如（“image/arrow.png”）
	 * @return Bitmap 图片
	 */
	public static Bitmap getBitmapFormSrc(String src){
		Bitmap bit = null;
		try {
			bit = BitmapFactory.decodeStream(AbFileUtil.class.getResourceAsStream(src));
	    } catch (Exception e) {
	    	 if(D)Log.d(TAG, "获取图片异常："+e.getMessage());
		}
	    if(D)Log.d(TAG, "返回的Bitmap："+bit);
		return bit;
	}
	
	/**
	 * 描述：获取网络文件的大小.
	 *
	 * @param Url 图片的网络路径
	 * @return int 网络文件的大小
	 */
	public static int getContentLengthFormUrl(String Url){
		int mContentLength = 0;
		try {
			 URL url = new URL(Url);
			 HttpURLConnection mHttpURLConnection = (HttpURLConnection) url.openConnection();
			 mHttpURLConnection.setConnectTimeout(5 * 1000);
			 mHttpURLConnection.setRequestMethod("GET");
			 mHttpURLConnection.setRequestProperty("Accept","image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			 mHttpURLConnection.setRequestProperty("Accept-Language", "zh-CN");
			 mHttpURLConnection.setRequestProperty("Referer", Url);
			 mHttpURLConnection.setRequestProperty("Charset", "UTF-8");
			 mHttpURLConnection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			 mHttpURLConnection.setRequestProperty("Connection", "Keep-Alive");
			 mHttpURLConnection.connect();
			 if (mHttpURLConnection.getResponseCode() == 200){
				 // 根据响应获取文件大小
				 mContentLength = mHttpURLConnection.getContentLength();
			 }
	    } catch (Exception e) {
	    	 e.printStackTrace();
	    	 if(D)Log.d(TAG, "获取长度异常："+e.getMessage());
		}
		return mContentLength;
	}
	
	 
	/**
	 * 获取文件名，通过网络获取.
	 * @param url 文件地址
	 * @return 文件名
	 */
	public static String getFileNameFromNetUrl(String url){
		String name = null;
		try {
			if(AbStrUtil.isEmpty(url)){
				return name;
			}
			
			URL mUrl = new URL(url);
			HttpURLConnection mHttpURLConnection = (HttpURLConnection) mUrl.openConnection();
			mHttpURLConnection.setConnectTimeout(5 * 1000);
			mHttpURLConnection.setRequestMethod("GET");
			mHttpURLConnection.setRequestProperty("Accept","image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			mHttpURLConnection.setRequestProperty("Accept-Language", "zh-CN");
			mHttpURLConnection.setRequestProperty("Referer", url);
			mHttpURLConnection.setRequestProperty("Charset", "UTF-8");
			mHttpURLConnection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			mHttpURLConnection.setRequestProperty("Connection", "Keep-Alive");
			mHttpURLConnection.connect();
			if (mHttpURLConnection.getResponseCode() == 200){
				for (int i = 0;; i++) {
						String mine = mHttpURLConnection.getHeaderField(i);
						if (mine == null){
							break;
						}
						if ("content-disposition".equals(mHttpURLConnection.getHeaderFieldKey(i).toLowerCase())) {
							Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
							if (m.find())
								return m.group(1).replace("\"", "");
						}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return name;
    }
	
	/**
	 * 获取文件名，通过网络获取.
	 * @param url 文件地址
	 * @return 文件名
	 */
	public static String getFileNameFromUrl(String url){
		if(AbStrUtil.isEmpty(url)){
			return null;
		}
		String name = null;
		try {
			name = AbMd5.MD5(url)+getSuffixFromUrl(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return name;
    }
	
	/**
	 * 获取文件名，通过网络获取.
	 * @param url 文件地址
	 * @param suffix .tmp
	 * @return 文件名
	 */
	public static String getFileNameFromUrl(String url,String suffix){
		if(AbStrUtil.isEmpty(url)){
			return null;
		}
		String name = null;
		try {
			name = AbMd5.MD5(url)+suffix;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return name;
    }
	
	/**
	 * 获取文件名，通过网络获取.
	 * @param url 文件地址
	 * @param suffix .tmp
	 * @return 文件名
	 */
	public static String getSuffixFromUrl(String url){
		if(AbStrUtil.isEmpty(url)){
			return null;
		}
		String suffix = ".tmp";
		try {
			//获取后缀
			if(url.lastIndexOf(".")!=-1){
				 suffix = url.substring(url.lastIndexOf("."));
				 if(suffix.indexOf("/")!=-1){
					 suffix = ".tmp";
				 }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return suffix;
    }
	
	/**
	 * 描述：从sd卡中的文件读取到byte[].
	 *
	 * @param path sd卡中文件路径
	 * @return byte[]
	 */
	public static byte[] getByteArrayFromSD(String path) {  
		byte[] bytes = null; 
		ByteArrayOutputStream out = null;
	    try {
	    	File file = new File(path);  
	    	//SD卡是否存在
			if(!isCanUseSD()){
				 return null;
		    }
			//文件是否存在
			if(!file.exists()){
				 return null;
			}
	    	
	    	long fileSize = file.length();  
	    	if (fileSize > Integer.MAX_VALUE) {  
	    	      return null;  
	    	}  

			FileInputStream in = new FileInputStream(path);  
		    out = new ByteArrayOutputStream(1024);  
			byte[] buffer = new byte[1024];  
			int size=0;  
			while((size=in.read(buffer))!=-1) {  
			   out.write(buffer,0,size);  
			}  
			in.close();  
            bytes = out.toByteArray();  
   
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(out!=null){
				try {
					out.close();
				} catch (Exception e) {
				}
			}
		}
	    return bytes;
    }  
	
	/**
	 * 描述：将byte数组写入文件.
	 *
	 * @param path the path
	 * @param content the content
	 * @param create the create
	 */
	 public static void writeByteArrayToSD(String path, byte[] content,boolean create){  
	    
		 FileOutputStream fos = null;
		 try {
	    	File file = new File(path);  
	    	//SD卡是否存在
			if(!isCanUseSD()){
				 return;
		    }
			//文件是否存在
			if(!file.exists()){
				if(create){
					File parent = file.getParentFile();
					if(!parent.exists()){
						parent.mkdirs();
						file.createNewFile();
					}
				}else{
				    return;
				}
			}
			fos = new FileOutputStream(path);  
			fos.write(content);  
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(fos!=null){
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
		}
   }  
	 
	/**
	 * 描述：SD卡是否能用.
	 *
	 * @return true 可用,false不可用
	 */
	public static boolean isCanUseSD() { 
	    try { 
	        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED); 
	    } catch (Exception e) { 
	        e.printStackTrace(); 
	    } 
	    return false; 
    } 

	/**
	 * 描述：获得当前下载的地址.
	 * @return 下载的地址（默认SD卡download）
	 */
	public static String getDownPathImageDir() {
		return downPathImageDir;
	}

	/**
	 * 描述：设置图片文件的下载保存路径（默认SD卡download/cache_images）.
	 * @param downPathImageDir 图片文件的下载保存路径
	 */
	public static void setDownPathImageDir(String downPathImageDir) {
		AbFileUtil.downPathImageDir = downPathImageDir;
	}

	
	/**
	 * Gets the down path file dir.
	 *
	 * @return the down path file dir
	 */
	public static String getDownPathFileDir() {
		return downPathFileDir;
	}

	/**
	 * 描述：设置文件的下载保存路径（默认SD卡download/cache_files）.
	 * @param downPathFileDir 文件的下载保存路径
	 */
	public static void setDownPathFileDir(String downPathFileDir) {
		AbFileUtil.downPathFileDir = downPathFileDir;
	}
	
	/**
	 * 描述：获取默认的图片保存全路径.
	 *
	 * @return the default image down path dir
	 */
	public static String getDefaultImageDownPathDir(){
		String pathDir = null;
	    try {
			if(!isCanUseSD()){
				return null;
			}
			//初始化图片保存路径
			File fileRoot = Environment.getExternalStorageDirectory();
			File dirFile = new File(fileRoot.getAbsolutePath() + AbFileUtil.downPathImageDir);
			if(!dirFile.exists()){
				dirFile.mkdirs();
			}
			pathDir = dirFile.getPath();
		} catch (Exception e) {
		}
	    return pathDir;
	}
	
	/**
    * 计算存储目录下的文件大小，
    * 当文件总大小大于规定的CACHE_SIZE或者sdcard剩余空间小于FREE_SD_SPACE_NEEDED_TO_CACHE的规定
    * 那么删除40%最近没有被使用的文件
    */
    public static boolean removeCache() {
    	
       try {
		   if(!isCanUseSD()){
				return false;
		   }
		   
		   File path = Environment.getExternalStorageDirectory();
		   File fileDirectory = new File(path.getAbsolutePath() + downPathImageDir);
	       File[] files = fileDirectory.listFiles();
	       if (files == null) {
	            return true;
	       }
	       if(dirSize==-1){
	    	   dirSize+=1;
	    	   for (int i = 0; i < files.length; i++) {
		            dirSize += files[i].length();
		       }
	       }
	       
	       //当前大小大于预定缓存空间
	       if (dirSize > cacheSize) {
	           int removeFactor = (int) ((0.4 * files.length) + 1);
	           Arrays.sort(files, new FileLastModifSort());
	           for (int i = 0; i < removeFactor; i++) {
	        	   dirSize -= files[i].length();
	               files[i].delete();
	           }
	        }
	       
	   } catch (Exception e) {
		   e.printStackTrace();
		   return false;
	   }
                                                       
       return true;
    }
	
    
    /**
     * 计算sdcard上的剩余空间
     */
    public static int freeSpaceOnSD() {
       StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
       double sdFreeMB = ((double)stat.getAvailableBlocks() * (double) stat.getBlockSize()) / MB;
       return (int) sdFreeMB;
    }
	
    /**
     * 根据文件的最后修改时间进行排序
     */
    public static class FileLastModifSort implements Comparator<File> {
        public int compare(File arg0, File arg1) {
            if (arg0.lastModified() > arg1.lastModified()) {
                return 1;
            } else if (arg0.lastModified() == arg1.lastModified()) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    /**
     * 
     * 描述：缓存文件夹的大小
     * @return
     * @throws 
     */
	public static int getCacheSize() {
		return cacheSize;
	}

	/**
	 * 
	 * 描述：设置缓存文件夹的大小
	 * @param cacheSize   B
	 * @throws 
	 */
	public static void setCacheSize(int cacheSize) {
		AbFileUtil.cacheSize = cacheSize;
	}

	/**
	 * 
	 * 描述：剩余空间大于多少B才使用缓存
	 * @return
	 * @throws 
	 */
	public static int getFreeSdSpaceNeededToCache() {
		return freeSdSpaceNeededToCache;
	}

	/**
	 * 
	 * 描述：剩余空间大于多少B才使用缓存
	 * @param freeSdSpaceNeededToCache
	 * @throws 
	 */
	public static void setFreeSdSpaceNeededToCache(int freeSdSpaceNeededToCache) {
		AbFileUtil.freeSdSpaceNeededToCache = freeSdSpaceNeededToCache;
	}
	
	/**
     * 删除所有缓存文件
    */
    public static boolean removeAllFileCache() {
    	
       try {
		   if(!isCanUseSD()){
				return false;
		   }
		   
		   File path = Environment.getExternalStorageDirectory();
		   File fileDirectory = new File(path.getAbsolutePath() + downPathImageDir);
	       File[] files = fileDirectory.listFiles();
	       if (files == null) {
	            return true;
	       }
           for (int i = 0; i < files.length; i++) {
               files[i].delete();
           }
	   } catch (Exception e) {
		   e.printStackTrace();
		   return false;
	   }
       return true;
    }
    
    /**
     * 根据url计算图片文件名称.
     * @param url 图片地址.
     * @param width 图片宽度.
     * @param height 图片高度.
     * @param type 处理类型.
     */
    public static String getImageFileName(String url, int width, int height,int type) {
        return AbMd5.MD5(new StringBuilder(url.length() + 12).append("#W").append(width)
        .append("#H").append(height).append("#T").append(type).append(url).toString());
    }
    
}
