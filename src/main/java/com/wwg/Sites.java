package com.wwg;

import java.util.*;

public class Sites{
    private HashMap<Integer, Store> siteMap;
    private int position;

    // Construct
    public Sites(){
        siteMap = new HashMap<>();
        position = 0;
    }

    // Add store function
    public void addStore(Integer site_number, Long site_ID, String name, Date onboard_date){
        Store s = new Store(site_ID, name, onboard_date);
        siteMap.put(site_number, s);
    }

    // Get by key
    public Store getStore(Integer site_number){
        if (siteMap.containsKey(site_number)){
            return siteMap.get(site_number);
        }
        System.out.println("Function <getStore> cannot find site: " + site_number);
        return null;
    }
    // Get store by ID
    public Store getStoreByID(Long site_id){
        for (Integer i : siteMap.keySet()){
            Store s = siteMap.get(i);
            if (s.getSite_ID().compareTo(site_id) == 0){
                return s;
            }
        }
        System.out.println("This SITE_ID is not found in the library: " + site_id);
        return null;
    }

    // Get array of Keys - Site_Number
    public Set<Integer> getSiteNumList(){
        return siteMap.keySet();
    }

    // Get size
    public int size(){
        return siteMap.size();
    }
}
