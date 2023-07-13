package com.rimo.sfcr;

import com.rimo.sfcr.config.CommonConfig;
import com.rimo.sfcr.config.CoreConfig;
import com.rimo.sfcr.core.Renderer;
import com.rimo.sfcr.core.Runtime;
import com.rimo.sfcr.network.Network;
import com.rimo.sfcr.register.Command;
import com.rimo.sfcr.register.Event;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFCReMod {

	public static final String MOD_ID = "sfcr";
	public static final Logger LOGGER = LoggerFactory.getLogger("sfcr");

	public static final ConfigHolder<CommonConfig> COMMON_CONFIG_HOLDER = AutoConfig.register(CommonConfig.class, GsonConfigSerializer::new);
	public static CommonConfig COMMON_CONFIG = COMMON_CONFIG_HOLDER.getConfig();

	public static final Runtime RUNTIME = new Runtime();
	public static Renderer RENDERER;

	public static void init() {
		Network.init();
		Event.register();
	}

	public static void initClient() {
		RENDERER = new Renderer();
		Network.initClient();
		Event.registerClient();
	}

	public static void initServer() {
		Command.register();
	}

	//Debug
	public static void exceptionCatcher(Exception e) {
		SFCReMod.LOGGER.error(e.toString());
		for (StackTraceElement i : e.getStackTrace()) {
			SFCReMod.LOGGER.error(i.getClassName() + ":" + i.getLineNumber());
		}
	}

}
