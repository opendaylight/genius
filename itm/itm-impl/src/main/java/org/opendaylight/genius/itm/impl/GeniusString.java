package org.opendaylight.genius.itm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public final class GeniusString {

    private static final Logger LOG = LoggerFactory.getLogger(GeniusString.class);

    private GeniusString() {
    }

    public static String[] stringSplit(String string2Split, char delimiter){
        LOG.debug("String to split:{}",string2Split);
        int previous_index = -1;
        int stringLength = string2Split.length();
        List<String> stringList = new LinkedList<>();
        for(int i=0; i<stringLength; i++){
            if(string2Split.charAt(i) == delimiter){
                stringList.add(string2Split.substring(++previous_index, i));
                previous_index = i;
            }
        }
        if(previous_index != (stringLength - 1)){
            stringList.add(string2Split.substring(++previous_index, stringLength));
        }
        LOG.debug("List after split:{}",stringList);
        String[] result = stringList.toArray(new String[0]);
        return result;
    }
}
