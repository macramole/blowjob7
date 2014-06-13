package art.leandrogarber.blowjob;

import java.io.*;
import java.net.*;
import java.util.*;   
import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.Display;
import android.widget.Toast;

public class Utils {

    /**
     * Convert byte array to hex string
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for(int idx=0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     * @param str
     * @return  array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     * @param filename
     * @return  
     * @throws java.io.IOException
     */
    public static String loadFileAsString(String filename) throws java.io.IOException {
        final int BUFLEN=1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8=false;
            int read,count=0;           
            while((read=is.read(bytes)) != -1) {
                if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
                    isUTF8=true;
                    baos.write(bytes, 3, read-3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count+=read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try{ is.close(); } catch(Exception ex){} 
        }
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface 
     * @return  mac address or empty string
     */
    /*public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));       
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }*/

    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                        if (useIPv4) {
                            if (isIPv4) 
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
    
    public static int mapRange(double value, double a1, double a2, double b1, double b2){
		double res =  b1 + ((value - a1)*(b2 - b1))/(a2 - a1);
		return (int)Math.floor(res);
	}
    
    public static Point getResolution(Activity context)
    {
    	Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point(display.getWidth(), display.getHeight());
        
        return size;
    }
    
    /**
     * Solo wifi encendido funciona
     * 
     * @param context
     * @return
     */
    public static String getMacAddress(Context context)
    {
    	WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    	WifiInfo info = manager.getConnectionInfo();
    	return info.getMacAddress();
    }
    
	public static String getServerIP(Context context) {
	        
		WifiManager wifii= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo d = wifii.getDhcpInfo();
	    
	    return intToIp( d.serverAddress );
	}

	private static String intToIp(int i)
	{
		return ((i >> 24 ) & 0xFF ) + "." +
	            ((i >> 16 ) & 0xFF) + "." +
	            ((i >> 8 ) & 0xFF) + "." +
	            ( i & 0xFF) ;
	}
}
