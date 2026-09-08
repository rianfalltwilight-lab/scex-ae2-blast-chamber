package cn.scex.ae2blast;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class BlastChamberBlock extends BaseEntityBlock {
    public static final MapCodec<BlastChamberBlock> CODEC = simpleCodec(BlastChamberBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    public BlastChamberBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false)); }
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ACTIVE); }
    public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new BlastChamberBlockEntity(p, s); }
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> t) {
        return level.isClientSide ? null : createTickerHelper(t, BlastChamberMod.ENTITY.get(), BlastChamberBlockEntity::tick);
    }
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos p, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(p) instanceof BlastChamberBlockEntity be) player.openMenu(be, p);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    protected void onRemove(BlockState s, Level level, BlockPos p, BlockState next, boolean moved) {
        if (!s.is(next.getBlock()) && level.getBlockEntity(p) instanceof BlastChamberBlockEntity be) {
            be.dropContents(); level.updateNeighbourForOutputSignal(p, this);
        }
        super.onRemove(s, level, p, next, moved);
    }
}
