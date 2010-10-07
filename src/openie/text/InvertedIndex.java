/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.text;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.HashMap;

public class InvertedIndex implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SERIAL_VERSION = 1;
	VMID instanceId = new VMID();  
	
	protected Alphabet dictX;
	protected Alphabet dictY;
	protected ArrayList<TIntIntHashMap> index;
	protected int nElements = 0;
	
	public InvertedIndex (Alphabet dictX, Alphabet dictY) {
		this.dictX = dictX;
		this.dictY = dictY;
		index = new ArrayList<TIntIntHashMap>(dictX.size());
	}
	
	public int lookup (int idY, int idX, boolean isUpdate) {
		int id = -1;
		
		if (idX < index.size()) {
			TIntIntHashMap map = index.get(idX);
			if (map.contains(idY)) 
				id = map.get(idY);
			else {
				id = nElements;
				map.put(idY, id);
				nElements++;
			}
		}
		else {
			TIntIntHashMap map = new TIntIntHashMap();
			id = nElements;
			map.put(idY, id);
			nElements++;
			index.add(map);
		}
		
		return id;
	}
	
	public int size () {
		return nElements;
	}
	
	public void clear () {
		index.clear();
	}
	
	public TIntIntHashMap getIndex (int i) {
		return index.get(i);
	}

	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeInt(index.size());
		for (int i = 0; i < index.size(); i++) {
			TIntIntHashMap map = index.get(i);
			out.writeInt(map.size());
			for (int key : map.keys()) {
				out.writeInt(key);
				out.writeInt(map.get(key));
			}
		}
		out.writeObject(instanceId);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		nElements = 0;
		int version = in.readInt();
		int size = in.readInt();
		index = new ArrayList<TIntIntHashMap>();
		for (int i = 0; i < size; i++) {
			int map_size = in.readInt();
			TIntIntHashMap map = new TIntIntHashMap();
			for (int j = 0; j < map_size; j++) {
				int key = in.readInt();
				int value = in.readInt();
				map.put(key, value);
				nElements++;
			}
			index.add(map);
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
