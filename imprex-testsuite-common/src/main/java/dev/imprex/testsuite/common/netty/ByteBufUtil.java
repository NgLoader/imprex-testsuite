package dev.imprex.testsuite.common.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

public class ByteBufUtil {

	public static int readVarInt(ByteBuf buffer) {
		int out = 0;
		int bytes = 0;
		byte in;
		do {
			in = buffer.readByte();
			out |= (in & 0x7F) << bytes++ * 7;
			if (bytes > 5) {
				throw new IndexOutOfBoundsException("varint32 too long");
			}
		} while ((in & 0x80) != 0);
		return out;
	}

	public static void writeVarInt(ByteBuf buffer, int value) {
		while ((value & -0x80) != 0) {
			buffer.writeByte(value & 0x7F | 0x80);
			value >>>= 7;
		}
		buffer.writeByte(value);
	}

	public static byte[] readByteArray(ByteBuf buffer) {
		byte[] array = new byte[ByteBufUtil.readVarInt(buffer)];
		buffer.readBytes(array);
		return array;
	}

	public static void writeByteArray(ByteBuf buffer, byte[] array) {
		ByteBufUtil.writeVarInt(buffer, array.length);
		buffer.writeBytes(array);
	}

	public static String readString(ByteBuf buffer) {
		return new String(ByteBufUtil.readByteArray(buffer));
	}

	public static void writeString(ByteBuf buffer, String message) {
		ByteBufUtil.writeByteArray(buffer, message.getBytes());
	}

	public static <T extends Enum<T>> T readEnum(ByteBuf buffer, Class<T> clazz) {
		return (T) clazz.getEnumConstants()[ByteBufUtil.readVarInt(buffer)];
	}

	public static void writeEnum(ByteBuf buffer, Enum<?> type) {
		ByteBufUtil.writeVarInt(buffer, type.ordinal());
	}

	public static UUID readUUID(ByteBuf buffer) {
		return new UUID(buffer.readLong(), buffer.readLong());
	}

	public static void writeUUID(ByteBuf buffer, UUID uuid) {
		buffer.writeLong(uuid.getMostSignificantBits());
		buffer.writeLong(uuid.getLeastSignificantBits());
	}

	public static InetSocketAddress readAddress(ByteBuf buffer) throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByAddress(ByteBufUtil.readByteArray(buffer)), buffer.readInt());
	}

	public static void writeAddress(ByteBuf buffer, InetSocketAddress address) {
		ByteBufUtil.writeByteArray(buffer, address.getAddress().getAddress());
		buffer.writeInt(address.getPort());
	}
}