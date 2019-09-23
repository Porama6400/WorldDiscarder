package net.otlg.worlddiscarder;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;

public class ReflectionUtil {
    public static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static String getChunkProviderClasspath() {
        return "net.minecraft.server." + getVersion() + ".ChunkProviderServer";
    }

    public static Class getChunkProviderServerClass(ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(ReflectionUtil.getChunkProviderClasspath(),true,loader);
    }

    public static Field getChunkProviderServerField(ClassLoader loader,String name) throws ClassNotFoundException, NoSuchFieldException {
        Field field = getChunkProviderServerClass(loader).getField(name);
        field.setAccessible(true);
        return field;
    }
}
