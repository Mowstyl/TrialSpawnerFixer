package com.clanjhoo.trialspawnerlootfix.capsule;

import com.clanjhoo.trialspawnerlootfix.TrialSpawnerLootFix;
import org.bukkit.Location;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;

public class WorldGuardCapsule {
    public static Object VAULT_KEY_TAG = null;
    private static Method getInstance;
    private static Method getPlatform;
    private static Method getRegionContainer;
    private static Method createQuery;
    private static Method getApplicableRegions;
    private static Method queryAllValues;
    private static Method queryValue;
    private static Method adaptLocation;

    public static void initializeFlags(TrialSpawnerLootFix plugin) {
        try {
            Class<?> worldGuardClazz = Class.forName("com.sk89q.worldguard.WorldGuard");
            getInstance = worldGuardClazz.getMethod("getInstance");
            getPlatform = worldGuardClazz.getMethod("getPlatform");
            Class<?> worldGuardPlatformClazz = Class.forName("com.sk89q.worldguard.internal.platform.WorldGuardPlatform");
            getRegionContainer = worldGuardPlatformClazz.getMethod("getRegionContainer");
            Class<?> regionContainerClazz = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
            createQuery = regionContainerClazz.getMethod("createQuery");
            Class<?> regionQueryClazz = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> locationClazz = Class.forName("com.sk89q.worldedit.util.Location");
            getApplicableRegions = regionQueryClazz.getMethod("getApplicableRegions", locationClazz);

            Class<?> flagClazz = Class.forName("com.sk89q.worldguard.protection.flags.Flag");
            Class<?> applicableRegionSetClazz = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> regionAssociableClazz = Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable");
            queryAllValues = applicableRegionSetClazz.getMethod("queryAllValues", regionAssociableClazz, flagClazz);
            queryValue = applicableRegionSetClazz.getMethod("queryValue", regionAssociableClazz, flagClazz);

            Class<?> bukkitAdapterClazz = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            adaptLocation = bukkitAdapterClazz.getMethod("adapt", Location.class);

            Method getFlagRegistry = worldGuardClazz.getMethod("getFlagRegistry");
            Iterable<?> registry = (Iterable<?>) getFlagRegistry.invoke(getInstance.invoke(null));

            Class<? extends Iterable<?>> flagRegistryClazz = (Class<? extends Iterable<?>>) Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
            Method register = flagRegistryClazz.getMethod("register", flagClazz);
            Method get = flagRegistryClazz.getMethod("get", String.class);

            Class<?> stringFlagClazz = Class.forName("com.sk89q.worldguard.protection.flags.StringFlag");
            Constructor<?> newStringFlag = stringFlagClazz.getConstructor(String.class, String.class);

            Object auxFlag;
            String flagName;
            flagName = "vault-key-tag";
            try {
                auxFlag = newStringFlag.newInstance(flagName, "");
                register.invoke(registry, auxFlag);
                VAULT_KEY_TAG = auxFlag; // only set our field if there was no error
            } catch (RuntimeException ex) {
                Class<?> flagConflictException = Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagConflictException");
                if (!flagConflictException.isInstance(ex))
                    throw ex;
                // some other plugin registered a flag by the same name already.
                // you can use the existing flag, but this may cause conflicts - be sure to check type
                Object existing = get.invoke(registry, flagName);
                if (stringFlagClazz.isInstance(existing)) {
                    VAULT_KEY_TAG = existing;
                } else {
                    // types don't match - this is bad news! some other plugin conflicts with you
                    // hopefully this never actually happens
                    plugin.log(Level.INFO, "Conflict found registering flags!");
                }
            }
            if (VAULT_KEY_TAG != null)
                plugin.log(Level.INFO, "Done registering WorldGuard flags.");
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException ex) {
            plugin.log(Level.INFO, "WorldGuard not found, flag support dropped.");
        }
    }

    public static Object getApplicableRegions(Location location) {
        try {
            Object weLocation = adaptLocation.invoke(null, location);
            return getApplicableRegions
                    .invoke(
                            createQuery.invoke(
                                    getRegionContainer.invoke(
                                            getPlatform.invoke(
                                                    getInstance.invoke(null)))),
                            weLocation);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            ex.printStackTrace();
            VAULT_KEY_TAG = null;
            return null;
        }
    }

    public static Collection<String> getFlagValues(Location location) {
        if (VAULT_KEY_TAG == null)
            return null;
        Object regionSet = getApplicableRegions(location);
        if (regionSet == null)
            return null;

        try {
            return (Collection<String>) queryAllValues.invoke(regionSet, null, VAULT_KEY_TAG);
        } catch (InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            ex.printStackTrace();
            VAULT_KEY_TAG = null;
            return null;
        }
    }

    public static String getFlagValue(Location location) {
        if (VAULT_KEY_TAG == null)
            return null;
        Object regionSet = getApplicableRegions(location);
        if (regionSet == null)
            return null;

        try {
            String value = (String) queryValue.invoke(regionSet, null, VAULT_KEY_TAG);
            if (value == null || value.isBlank())
                value = null;
            return value;
        } catch (InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            ex.printStackTrace();
            VAULT_KEY_TAG = null;
            return null;
        }
    }
}
