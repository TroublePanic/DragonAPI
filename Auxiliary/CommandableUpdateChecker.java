/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.Auxiliary;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Command.DragonCommandBase;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.Java.ReikaStringParser;
import cpw.mods.fml.common.Loader;

public class CommandableUpdateChecker {

	public static final CommandableUpdateChecker instance = new CommandableUpdateChecker();

	public static final String reikaURL = "http://server.techjargaming.com/Reika/versions";

	private final HashMap<DragonAPIMod, ModVersion> latestVersions = new HashMap();
	private final ArrayList<UpdateChecker> checkers = new ArrayList();
	private final ArrayList<DragonAPIMod> oldMods = new ArrayList();
	private final HashMap<String, DragonAPIMod> modNames = new HashMap();
	private final HashMap<DragonAPIMod, Boolean> overrides = new HashMap();

	private CommandableUpdateChecker() {

	}

	public void checkAll() {
		this.getOverrides();
		for (int i = 0; i < checkers.size(); i++) {
			UpdateChecker c = checkers.get(i);
			DragonAPIMod mod = c.mod;
			if (this.shouldCheck(mod)) {
				ModVersion version = c.version;
				ModVersion latest = latestVersions.get(mod);
				//if (version.isCompiled()) {
				if (version.equals(latest)) {

				}
				else {
					this.markUpdate(mod);
					ReikaJavaLibrary.pConsole("-----------------------"+mod.getTechnicalName()+"-----------------------");
					ReikaJavaLibrary.pConsole("This version of the mod ("+version+") is out of date.");
					ReikaJavaLibrary.pConsole("This version is likely to contain bugs, crashes, and/or exploits.");
					ReikaJavaLibrary.pConsole("No technical support whatsoever will be provided for this version.");
					ReikaJavaLibrary.pConsole("Update to "+latest+" as soon as possible.");
					ReikaJavaLibrary.pConsole("------------------------------------------------------------------------");
					ReikaJavaLibrary.pConsole("");
				}
				//}
				//else {
				//
				//}
			}
		}
	}

	private void markUpdate(DragonAPIMod mod) {
		oldMods.add(mod);
	}

	public void registerMod(DragonAPIMod mod) {
		ModVersion version = this.getVersion(mod);
		if (version == null) {
			mod.getModLogger().log("Mod is in source code form. Not checking versions.");
			return;
		}
		String url = mod.getUpdateCheckURL()+"_"+Loader.MC_VERSION.replaceAll("\\.", "-")+".txt";
		URL file = this.getURL(url);
		if (file == null) {
			mod.getModLogger().logError("Could not create URL to update checker. Version will not be checked.");
			return;
		}
		UpdateChecker c = new UpdateChecker(mod, version, file);
		ModVersion latest = c.getLatestVersion();
		if (latest == null) {
			mod.getModLogger().logError("Could not access online version reference. Please notify "+mod.getModAuthorName());
			return;
		}
		latestVersions.put(mod, latest);
		checkers.add(c);
		String label = ReikaStringParser.stripSpaces(mod.getDisplayName().toLowerCase());
		modNames.put(label, mod);
	}

	private URL getURL(String url) {
		try {
			return new URL(url);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean shouldCheck(DragonAPIMod mod) {
		return overrides.containsKey(mod) ? overrides.get(mod) : true;
	}

	private void getOverrides() {
		File f = this.getFile();
		if (f.exists()) {
			ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
			for (int i = 0; i < li.size(); i++) {
				String line = li.get(i);
				String[] parts = line.split(":");
				DragonAPIMod mod = modNames.get(parts[0]);
				boolean b = Boolean.parseBoolean(parts[1]);
				ModVersion version = ModVersion.getFromString(parts[2]);
				if (version.equals(latestVersions.get(mod)))
					overrides.put(mod, b);
			}
		}
	}

	private ModVersion getVersion(DragonAPIMod mod) {
		//if (name.equals("bin"))
		//	return ModVersion.source;
		String major = mod.getMajorVersion();
		String minor = mod.getMinorVersion();
		if (major.startsWith("@")) { //dev environment
			return null;
		}
		return new ModVersion(Integer.parseInt(major), minor.isEmpty() ? '\0' : minor.charAt(0));
	}

	private void setChecker(DragonAPIMod mod, boolean enable) {
		File f = this.getFile();
		String name = ReikaStringParser.stripSpaces(mod.getDisplayName().toLowerCase());
		ModVersion latest = latestVersions.get(mod);
		if (f.exists()) {
			ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
			Iterator<String> it = li.iterator();
			while (it.hasNext()) {
				String line = it.next();
				if (line.startsWith(name)) {
					it.remove();
				}
			}
			li.add(name+":"+enable+":"+latest);
			try {
				PrintWriter p = new PrintWriter(f);
				for (int i = 0; i < li.size(); i++)
					p.append(li.get(i)+"\n");
				p.close();
			}
			catch (IOException e) {

			}
		}
		else {
			try {
				f.createNewFile();
				PrintWriter p = new PrintWriter(f);
				p.append(name+":"+enable+":"+latest);
				p.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private File getFile() {
		String base = DragonAPICore.getMinecraftDirectory().getAbsolutePath();
		File parent = new File(base+"/saves/DragonAPI");
		if (!parent.exists())
			parent.mkdirs();
		String path = parent+"/ucheck.dat";
		File f = new File(path);
		return f;
	}

	public void notifyPlayer(EntityPlayer ep) {
		if (!oldMods.isEmpty()) {
			String sg = EnumChatFormatting.YELLOW.toString()+"DragonAPI Notification:";
			ReikaChatHelper.sendChatToPlayer(ep, sg);
		}
		for (int i = 0; i < oldMods.size(); i++) {
			DragonAPIMod mod = oldMods.get(i);
			String s = this.getChatMessage(mod);
			ReikaChatHelper.sendChatToPlayer(ep, s);
		}
		if (!oldMods.isEmpty()) {
			String g = EnumChatFormatting.YELLOW.toString();
			String sg = g+"To disable this notifcation for any mod, type \"/"+CheckerDisableCommand.tag+" disable [modname]\".";
			ReikaChatHelper.sendChatToPlayer(ep, sg);
			sg = g+"Changes take effect upon server or client restart.";
			ReikaChatHelper.sendChatToPlayer(ep, sg);
		}
	}

	private String getChatMessage(DragonAPIMod mod) {
		ModVersion latest = latestVersions.get(mod);
		String g = EnumChatFormatting.LIGHT_PURPLE.toString();
		String r = EnumChatFormatting.RESET.toString();
		return g+mod.getDisplayName()+r+" is out of date, likely has errors, and is no longer supported. Update to "+latest+".";
	}

	public static class CheckerDisableCommand extends DragonCommandBase {

		public static final String tag = "checker";

		@Override
		public String getCommandString() {
			return tag;
		}

		@Override
		public void processCommand(ICommandSender ics, String[] args) {
			EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
			if (args.length == 2) {
				String action = args[0];
				String name = args[1];
				DragonAPIMod mod = instance.modNames.get(name);
				if (mod != null) {
					if (action.equals("disable")) {
						instance.setChecker(mod, false);
						String sg = EnumChatFormatting.BLUE.toString()+"Update checker for "+mod.getDisplayName()+" disabled.";
						ReikaChatHelper.sendChatToPlayer(ep, sg);
					}
					else if (action.equals("enable")) {
						instance.setChecker(mod, true);
						String sg = EnumChatFormatting.BLUE.toString()+"Update checker for "+mod.getDisplayName()+" enabled.";
						ReikaChatHelper.sendChatToPlayer(ep, sg);
					}
					else {
						String sg = EnumChatFormatting.RED.toString()+"Invalid argument '"+action+"'.";
						ReikaChatHelper.sendChatToPlayer(ep, sg);
					}
				}
				else {
					String sg = EnumChatFormatting.RED.toString()+"Mod '"+name+"' not found.";
					ReikaChatHelper.sendChatToPlayer(ep, sg);
				}
			}
			else {
				String sg = EnumChatFormatting.RED.toString()+"Invalid arguments.";
				ReikaChatHelper.sendChatToPlayer(ep, sg);
			}
		}
	}

	private static class UpdateChecker {

		private final ModVersion version;
		private final URL checkURL;
		private final DragonAPIMod mod;

		private UpdateChecker(DragonAPIMod mod, ModVersion version, URL url) {
			this.mod = mod;
			this.version = version;
			checkURL = url;
		}

		private ModVersion getLatestVersion() {
			try {
				ArrayList<String> lines = ReikaFileReader.getFileAsLines(checkURL, false);
				String name = ReikaStringParser.stripSpaces(mod.getDisplayName().toLowerCase());
				for (int i = 0; i < lines.size(); i++) {
					String line = lines.get(i);
					if (line.toLowerCase().startsWith(name)) {
						String[] parts = line.split(":");
						String part = parts[1];
						ModVersion version = ModVersion.getFromString(part);
						return version;
					}
				}
			}
			catch (Exception e) {
				if (e instanceof IOException) {
					mod.getModLogger().logError("IO Error accessing online file:");
					mod.getModLogger().log(e.getClass().getCanonicalName()+": "+e.getLocalizedMessage());
					mod.getModLogger().log(e.getStackTrace()[0].toString());
				}
				else {
					mod.getModLogger().logError("Error accessing online file:");
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	private static class ModVersion {
		/*
		public static final ModVersion source = new ModVersion(0) {
			@Override
			public boolean equals(Object o) {
				return o instanceof ModVersion;
			}
			@Override
			public String toString() {
				return "Source Code";
			}
			@Override
			public boolean isCompiled() {
				return false;
			}
		};*/

		public final int majorVersion;
		public final String subVersion;

		private ModVersion(int major) {
			this(major, '\0');
		}

		private ModVersion(int major, char minor) {
			majorVersion = major;
			subVersion = minor == '\0' ? "" : Character.toString(minor).toLowerCase();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ModVersion) {
				ModVersion m = (ModVersion)o;
				return m.majorVersion == majorVersion && m.subVersion.equals(subVersion);
			}
			return false;
		}

		public boolean isCompiled() {
			return true;
		}

		@Override
		public String toString() {
			return "v"+majorVersion+subVersion;
		}

		public static ModVersion getFromString(String s) {
			if (s.startsWith("v") || s.startsWith("V"))
				s = s.substring(1);
			char c = s.charAt(s.length()-1);
			if (Character.isDigit(c)) {
				return new ModVersion(Integer.parseInt(s));
			}
			else {
				String major = s.substring(0, s.length()-1);
				return new ModVersion(Integer.parseInt(major), c);
			}
		}
	}

}
