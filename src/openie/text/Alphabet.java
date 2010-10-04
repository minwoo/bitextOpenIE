/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.text;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.rmi.dgc.VMID;
import gnu.trove.map.hash.TObjectIntHashMap;

public class Alphabet implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 1;
	private static final int DEFAULT_CAPACITY = 8;
	VMID instanceId = new VMID();  
	
	protected TObjectIntHashMap map;
    protected ArrayList list;

    public Alphabet () {
        this(DEFAULT_CAPACITY);
    }

	public Alphabet (int capacity) {
        this.map = new TObjectIntHashMap(capacity);
        this.list = new ArrayList(capacity);
    }

	public int lookup (Object entry, boolean isUpdate) {
        if (entry == null)
            throw new IllegalArgumentException("Can't find \"null\" entry in Dictionary");
        
        int ret = -1;
        if (map.containsKey(entry)) 
            ret = map.get(entry);
        else if (isUpdate) {
            ret = list.size();
            map.put(entry, ret);
            list.add(entry);
        }
        
        return ret;
    }

    public int lookup (Object entry) {
        return lookup(entry, true);
    }

    public int[] lookup (Object[] entries, boolean isUpdate) {
        int[] ret = new int[entries.length];
        
        for (int i = 0; i < entries.length; i++)
            ret[i] = lookup(entries[i], isUpdate);
        
        return ret;
    }

    public Object getObject (int index) {
        return list.get(index);
    }

    public Object[] getObject (int[] indices) {
        Object[] ret = new Object[indices.length];
        
        for (int i = 0; i < indices.length; i++)
            ret[i] = list.get(i);
        
        return ret;
    }

    public boolean contains (Object entry) {
        return map.containsKey(entry);
    }

    public int size () {
        return list.size();
    }

    public String toString () {
        StringBuffer sb = new StringBuffer();
        
        for (Object i : list) {
            sb.append(i.toString());
            sb.append('\n');
        }
        
        return sb.toString();
    }
    
    public Object[] toArray () {
        return list.toArray();
    }
    
    public void clear () {
    	map.clear();
    	list.clear();
    }
    
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeInt(list.size());
		for (int i = 0; i < list.size(); i++)
			out.writeObject(list.get(i));
		out.writeObject(instanceId);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		int size = in.readInt();
		list = new ArrayList(size);
		map = new TObjectIntHashMap(size);
		for (int i = 0; i < size; i++) {
			Object o = in.readObject();
			map.put(o, i);
			list.add(o);
		}
		if (version > 0 ) 
			instanceId = (VMID) in.readObject();
	}

	private transient static HashMap deserializedEntries = new HashMap();
 
	public Object readResolve() throws ObjectStreamException {
		Object previous = deserializedEntries.get(instanceId);
		
		if (previous != null)
			return previous;
		if (instanceId != null)
			deserializedEntries.put(instanceId, this);
		
		return this;
	}
}