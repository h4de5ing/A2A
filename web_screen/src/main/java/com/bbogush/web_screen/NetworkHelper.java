package com.bbogush.web_screen;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.util.LinkedList;
import java.util.List;

public class NetworkHelper {
    public static class IpInfo {
        public String interfaceName;
        public List<LinkAddress> addresses;
    }

    public static List<IpInfo> getIpInfo(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        List<IpInfo> ipInfoList = new LinkedList<>();

        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            NetworkCapabilities networkCapabilities =
                    connectivityManager.getNetworkCapabilities(network);

            if (linkProperties == null || networkCapabilities == null) {
                continue;
            }

            String interfaceName = linkProperties.getInterfaceName();
            if (interfaceName == null) {
                continue;
            }

            IpInfo ipInfo = new IpInfo();
            ipInfo.interfaceName = interfaceName;
            ipInfo.addresses = new LinkedList<>();
            List<LinkAddress> addresses = linkProperties.getLinkAddresses();
            for (LinkAddress address : addresses) {
                if (address.getAddress().isLinkLocalAddress())
                    continue;
                ipInfo.addresses.add(address);
            }

            ipInfoList.add(ipInfo);
        }

        return ipInfoList;
    }
}
