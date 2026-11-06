package com.cheba.util;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.internal.FMLMessage;
import noppes.npcs.LogWriter;

import java.lang.reflect.Method;

public class EntitySpawnMessageHelper {

    private static final String TAG = "[EntitySpawnMessageHelper] ";

    private static Method toBytesMethod;
    private static Method fromBytesMethod;
    private static Method spawnMethod;
    private static Object handlerInstance;

    static {
        try {
            toBytesMethod = FMLMessage.EntitySpawnMessage.class
                    .getDeclaredMethod("toBytes", ByteBuf.class);
            toBytesMethod.setAccessible(true);

            fromBytesMethod = FMLMessage.EntitySpawnMessage.class
                    .getDeclaredMethod("fromBytes", ByteBuf.class);
            fromBytesMethod.setAccessible(true);

            // Клиентские штуки инициализируем ТОЛЬКО на клиенте
            if (FMLCommonHandler.instance().getSide().isClient()) {
                try {
                    Class<?> handlerClass = Class.forName(
                            "net.minecraftforge.fml.common.network.internal.EntitySpawnHandler");
                    handlerInstance = handlerClass.getDeclaredConstructor().newInstance();

                    for (Method m : handlerClass.getDeclaredMethods()) {
                        if (m.getParameterCount() == 1
                                && m.getParameterTypes()[0] == FMLMessage.EntitySpawnMessage.class) {
                            spawnMethod = m;
                            spawnMethod.setAccessible(true);
                            break;
                        }
                    }
                } catch (Throwable t) {
                    LogWriter.error(TAG + "Client-side init failed", t);
                }
            }
        } catch (Throwable t) {
            LogWriter.error(TAG + "Core init failed", t);
        }
    }

    public static void toBytes(FMLMessage.EntitySpawnMessage msg, ByteBuf buf) {
        try {
            if (toBytesMethod == null) {
                LogWriter.error(TAG + "toBytes skipped: toBytesMethod is null (init failed)");
                return;
            }
            toBytesMethod.invoke(msg, buf);
        } catch (Throwable t) {
            LogWriter.error(TAG + "toBytes failed", t);
        }
    }

    public static void spawn(ByteBuf buffer) {
        try {
            if (fromBytesMethod == null || spawnMethod == null || handlerInstance == null) {
                LogWriter.error(TAG + "spawn skipped: helper fields are null "
                        + "(fromBytes=" + fromBytesMethod
                        + ", spawn=" + spawnMethod
                        + ", handler=" + handlerInstance + ")");
                return;
            }
            FMLMessage.EntitySpawnMessage msg = new FMLMessage.EntitySpawnMessage();
            fromBytesMethod.invoke(msg, buffer);
            spawnMethod.invoke(handlerInstance, msg);
        } catch (Throwable t) {
            LogWriter.error(TAG + "spawn failed on client", t);
        }
    }
}
