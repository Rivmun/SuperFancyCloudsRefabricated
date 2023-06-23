package com.rimo.sfcr.network;

import java.io.*;
import java.util.Optional;

public class DataSerializer<T> {
	public byte[] serialize(T config) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(config);
			out.flush();
			return bos.toByteArray();
		} catch (IOException ex) {
			return new byte[0];
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {
				return new byte[0];
			}
		}
	}
	public Optional<T> deserialize(byte[] from) {
		ByteArrayInputStream bis = new ByteArrayInputStream(from);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			return Optional.ofNullable((T) in.readObject());
		} catch (IOException | ClassNotFoundException ex) {
			return Optional.empty();
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException ex) {
				return Optional.empty();
			}
		}
	}
}
