/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.mana;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.mana.IManaTrigger;
import vazkii.botania.common.block.BlockMod;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.item.ItemHorn;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockForestDrum extends BlockMod implements IManaTrigger {

	public enum Variant {
		WILD,
		GATHERING,
		CANOPY
	}

	private static final VoxelShape SHAPE = Block.makeCuboidShape(3, 1, 3, 13, 15, 13);
	private final Variant variant;

	public BlockForestDrum(Variant v, Properties builder) {
		super(builder);
		this.variant = v;
	}

	@Nonnull
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
		return SHAPE;
	}

	@Override
	public void onBurstCollision(IManaBurst burst, World world, BlockPos pos) {
		if (burst.isFake()) {
			return;
		}
		if (world.isRemote) {
			world.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5D, 1.0 / 24.0, 0, 0);
			return;
		}
		if (variant == Variant.WILD) {
			ItemHorn.breakGrass(world, new ItemStack(ModItems.grassHorn), pos);
		} else if (variant == Variant.CANOPY) {
			ItemHorn.breakGrass(world, new ItemStack(ModItems.leavesHorn), pos);
		} else {
			int range = 10;
			List<MobEntity> entities = world.getEntitiesWithinAABB(MobEntity.class, new AxisAlignedBB(pos.add(-range, -range, -range), pos.add(range + 1, range + 1, range + 1)));
			List<MobEntity> shearables = new ArrayList<>();
			ItemStack stack = new ItemStack(ModBlocks.gatheringDrum);

			for (MobEntity entity : entities) {
				if (entity instanceof IShearable && ((IShearable) entity).isShearable(stack, world, new BlockPos(entity))) {
					shearables.add(entity);
				} else if (entity instanceof CowEntity) {
					List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class, entity.getBoundingBox());
					for (ItemEntity item : items) {
						ItemStack itemstack = item.getItem();
						if (!itemstack.isEmpty() && itemstack.getItem() == Items.BUCKET && !world.isRemote) {
							while (itemstack.getCount() > 0) {
								ItemEntity ent = entity.entityDropItem(new ItemStack(Items.MILK_BUCKET), 1.0F);
								ent.setMotion(ent.getMotion().add(
										world.rand.nextFloat() * 0.05F,
										(world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F,
										(world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F
								));
								itemstack.shrink(1);
							}
							item.remove();
						}
					}
				}
			}

			Collections.shuffle(shearables);
			int sheared = 0;

			for (MobEntity entity : shearables) {
				if (sheared > 4) {
					break;
				}

				List<ItemStack> stacks = ((IShearable) entity).onSheared(stack, world, new BlockPos(entity), 0);
				for (ItemStack wool : stacks) {
					ItemEntity ent = entity.entityDropItem(wool, 1.0F);
					ent.setMotion(ent.getMotion().add(
							world.rand.nextFloat() * 0.05F,
							(world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F,
							(world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F
					));
				}
				++sheared;
			}
		}

		for (int i = 0; i < 10; i++) {
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.BLOCKS, 1F, 1F);
		}
	}
}
