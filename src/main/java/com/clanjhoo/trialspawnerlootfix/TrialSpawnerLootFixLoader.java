package com.clanjhoo.trialspawnerlootfix;


import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.jetbrains.annotations.NotNull;

public class TrialSpawnerLootFixLoader implements PluginLoader {

	@Override
	public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {
		MavenLibraryResolver resolver = new MavenLibraryResolver();
		resolver.addDependency(new Dependency(new DefaultArtifact("${libs.kyori.adventure.minimessage.get()}"), null));
		resolver.addDependency(new Dependency(new DefaultArtifact("${libs.kyori.adventure.gson.get()}"), null));
		resolver.addDependency(new Dependency(new DefaultArtifact("${libs.kyori.platform.bukkit.get()}"), null));
	}
}