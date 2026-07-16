package com.dannykim.dtbetterend.client;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraftforge.client.textures.ForgeTextureMetadata;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

public final class ThickBranchRingsSource implements SpriteSource {
    public static final ResourceLocation ID = new ResourceLocation("dynamictrees", "thick_branch_rings");
    public static final Codec<ThickBranchRingsSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(source -> source.resourceId)
    ).apply(instance, ThickBranchRingsSource::new));

    private static final ThickBranchRingsSpriteLoader LOADER = new ThickBranchRingsSpriteLoader();
    private static SpriteSourceType TYPE;

    private final ResourceLocation resourceId;

    public ThickBranchRingsSource(final ResourceLocation resourceId) {
        this.resourceId = resourceId;
    }

    public static synchronized void register() {
        if (TYPE != null) {
            return;
        }
        try {
            final Field field = findSpriteSourceTypesField();
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final BiMap<ResourceLocation, SpriteSourceType> types =
                    (BiMap<ResourceLocation, SpriteSourceType>) field.get(null);
            TYPE = types.get(ID);
            if (TYPE == null) {
                TYPE = new SpriteSourceType(CODEC);
                types.put(ID, TYPE);
            }
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register Dynamic Trees thick branch rings source.", exception);
        }
    }

    private static Field findSpriteSourceTypesField() throws NoSuchFieldException {
        for (final Field field : SpriteSources.class.getDeclaredFields()) {
            if (BiMap.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        throw new NoSuchFieldException("SpriteSources BiMap");
    }

    @Override
    public void run(final ResourceManager resourceManager, final Output output) {
        final ResourceLocation textureId = TEXTURE_ID_CONVERTER.idToFile(this.resourceId);
        final Optional<Resource> resource = resourceManager.getResource(textureId);
        if (resource.isPresent()) {
            final ResourceLocation thickId = new ResourceLocation(this.resourceId.getNamespace(), this.resourceId.getPath() + "_thick");
            output.add(thickId, wrap(resource.get()));
        }
    }

    @Override
    public SpriteSourceType type() {
        register();
        return TYPE;
    }

    private static Resource wrap(final Resource original) {
        return new Resource(original.source(), original::open, () -> new ThickBranchMetadata(original));
    }

    private static final class ThickBranchMetadata implements ResourceMetadata {
        private final Resource original;

        private ThickBranchMetadata(final Resource original) {
            this.original = original;
        }

        @Override
        public <T> Optional<T> getSection(final MetadataSectionSerializer<T> serializer) {
            if (serializer == ForgeTextureMetadata.SERIALIZER) {
                return Optional.of((T) new ForgeTextureMetadata(LOADER));
            }
            try {
                return this.original.metadata().getSection(serializer);
            } catch (final IOException exception) {
                return Optional.empty();
            }
        }
    }
}
