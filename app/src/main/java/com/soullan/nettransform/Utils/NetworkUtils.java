package com.soullan.nettransform.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkUtils {
    public static String getIpAddressString() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAdd = netI
                        .getInetAddresses(); enumIpAdd.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAdd.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }

    /*判断字符串是否为一个合法的IPv4地址*/
    public static boolean isIPv4Address(String address) {
        String regex = "(2[5][0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})"
                        + "\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})"
                        + "\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})"
                        + "\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(address);
        return m.matches();
    }

    public static boolean isIPv4Port(int port) {
        return port >= 0 && port < 65536;
    }

    public static boolean isInteger(String str) {
        if (str.equals("")) return false;
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }
}
