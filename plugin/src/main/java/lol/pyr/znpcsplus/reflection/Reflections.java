package lol.pyr.znpcsplus.reflection;

import com.mojang.authlib.GameProfile;
import lol.pyr.znpcsplus.reflection.types.ClassReflection;
import lol.pyr.znpcsplus.reflection.types.FieldReflection;
import lol.pyr.znpcsplus.reflection.types.MethodReflection;
import lol.pyr.znpcsplus.util.VersionUtil;
import lol.pyr.znpcsplus.util.FoliaUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Class containing all of the lazy-loaded reflections that the plugin
 * uses to accessinaccessible things from the server jar.
 */
public final class Reflections {
    public static final Class<?> ENTITY_CLASS = new ClassReflection(
            new ReflectionBuilder(ReflectionPackage.ENTITY)
                    .withClassName("Entity")).get();

    public static final Class<?> ENTITY_HUMAN_CLASS = new ClassReflection(
            new ReflectionBuilder(ReflectionPackage.ENTITY)
                    .withSubClass("player")
                    .withClassName("EntityHuman")).get();

    public static final ReflectionLazyLoader<Method> GET_PROFILE_METHOD = new MethodReflection(
            new ReflectionBuilder(ReflectionPackage.ENTITY)
                    .withClassName(ENTITY_HUMAN_CLASS)
                    .withExpectResult(GameProfile.class));

    public static final ReflectionLazyLoader<Method> GET_HANDLE_PLAYER_METHOD = new MethodReflection(
            new ReflectionBuilder(ReflectionPackage.BUKKIT)
                    .withClassName("entity.CraftPlayer").withClassName("entity.CraftHumanEntity")
                    .withMethodName("getHandle"));

    public static final FieldReflection.ValueModifier<Integer> ENTITY_ID_MODIFIER = new FieldReflection(
            new ReflectionBuilder(ReflectionPackage.ENTITY)
                    .withClassName(ENTITY_CLASS)
                    .withFieldName("entityCount")
                    .setStrict(!VersionUtil.isNewerThan(14))).staticValueModifier(int.class);

    public static final ReflectionLazyLoader<AtomicInteger> ATOMIC_ENTITY_ID_FIELD = new FieldReflection(
            new ReflectionBuilder(ReflectionPackage.ENTITY)
                    .withClassName(ENTITY_CLASS)
                    .withFieldName("entityCount")
                    .withFieldName("d")
                    .withFieldName("c")
                    .withExpectResult(AtomicInteger.class)
                    .setStrict(VersionUtil.isNewerThan(14))).staticValueLoader(AtomicInteger.class);

    public static final Class<?> ASYNC_SCHEDULER_CLASS = new ClassReflection(
            new ReflectionBuilder("io.papermc.paper.threadedregions.scheduler")
                    .withClassName("AsyncScheduler")
                    .setStrict(FoliaUtil.isFolia())).get();

    public static final Class<?> SCHEDULED_TASK_CLASS = new ClassReflection(
            new ReflectionBuilder("io.papermc.paper.threadedregions.scheduler")
                    .withClassName("ScheduledTask")
                    .setStrict(FoliaUtil.isFolia())).get();

    public static final ReflectionLazyLoader<Method> FOLIA_GET_ASYNC_SCHEDULER = new MethodReflection(
            new ReflectionBuilder(Bukkit.class)
                    .withMethodName("getAsyncScheduler")
                    .withExpectResult(ASYNC_SCHEDULER_CLASS)
                    .setStrict(FoliaUtil.isFolia()));

    public static final ReflectionLazyLoader<Method> FOLIA_RUN_NOW = new MethodReflection(
            new ReflectionBuilder(ASYNC_SCHEDULER_CLASS)
                    .withMethodName("runNow")
                    .withParameterTypes(Plugin.class, Consumer.class)
                    .withExpectResult(SCHEDULED_TASK_CLASS)
                    .setStrict(FoliaUtil.isFolia()));

    public static final ReflectionLazyLoader<Method> FOLIA_RUN_DELAYED = new MethodReflection(
            new ReflectionBuilder(ASYNC_SCHEDULER_CLASS)
                    .withMethodName("runDelayed")
                    .withParameterTypes(Plugin.class, Consumer.class, long.class, TimeUnit.class)
                    .withExpectResult(SCHEDULED_TASK_CLASS)
                    .setStrict(FoliaUtil.isFolia()));

    public static final ReflectionLazyLoader<Method> FOLIA_RUN_AT_FIXED_RATE = new MethodReflection(
            new ReflectionBuilder(ASYNC_SCHEDULER_CLASS)
                    .withMethodName("runAtFixedRate")
                    .withParameterTypes(Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class)
                    .withExpectResult(SCHEDULED_TASK_CLASS)
                    .setStrict(FoliaUtil.isFolia()));
}