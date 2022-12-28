package com.critmxbelvix.simplyrs.common.blocks;

import com.critmxbelvix.simplyrs.common.creativetabs.SimplyRSCreativeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

public class RedstoneCrossbridge extends Block {

    final static String name = "redstone_crossbridge";
    final static CreativeModeTab tab = SimplyRSCreativeTab.SRS_TAB;
    final static Properties crossbridge_properties = Properties.of(Material.STONE).strength(0.3f).dynamicShape();

    private static final VoxelShape SHAPE = Stream.of(
            Block.box(0.25, 0, 0.25, 15.75, 1, 15.75),
            Block.box(0.5, 1, 0.5, 15.5, 3, 15.5),
            Block.box(7, 2, 0, 7.25, 4, 7),
            Block.box(8.75, 2, 0, 9, 4, 7),
            Block.box(0, 2, 7, 7.25, 4, 7.25),
            Block.box(0, 2, 8.75, 7.25, 4, 9),
            Block.box(7, 2, 9, 7.25, 4, 16),
            Block.box(8.75, 2, 9, 9, 4, 16),
            Block.box(8.75, 2, 8.75, 16, 4, 9),
            Block.box(8.75, 2, 7, 16, 4, 7.25),
            Block.box(0, 0, 7, 0.25, 2, 9),
            Block.box(7, 0, 0, 9, 2, 0.25),
            Block.box(7, 0, 15.75, 9, 2, 16),
            Block.box(15.75, 0, 7, 16, 2, 9)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty INPUT_N = BooleanProperty.create("input_n");
    public static final BooleanProperty INPUT_E = BooleanProperty.create("input_e");
    public static final BooleanProperty INPUT_S = BooleanProperty.create("input_s");
    public static final BooleanProperty INPUT_W = BooleanProperty.create("input_w");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public RedstoneCrossbridge(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(INPUT_N,false)
                        .setValue(INPUT_E, false)
                        .setValue(INPUT_W, false)
                        .setValue(INPUT_S,false)
        );
    }

    public static String m_getName()
    {
        return name;
    }
    public static CreativeModeTab m_getTab()
    {
        return tab;
    }
    public static Properties m_getProperties()
    {
        return crossbridge_properties;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canSupportRigidBlock(pLevel, pPos.below());
    }

    // Blockstates

    public BlockState getStateForPlacement(BlockPlaceContext pContext)
    {
        BlockState blockstate = super.getStateForPlacement(pContext);
        Direction north = pContext.getHorizontalDirection();
        Direction west = north.getCounterClockWise();
        Direction south = north.getOpposite();
        Direction east = north.getClockWise();

        return this.defaultBlockState()
                .setValue(FACING,pContext.getHorizontalDirection())
                .setValue(INPUT_N,isInputNorth(pContext.getLevel(),pContext.getClickedPos().relative(north),blockstate))
                .setValue(INPUT_E,isInputEast(pContext.getLevel(),pContext.getClickedPos().relative(east),blockstate))
                .setValue(INPUT_W,isInputWest(pContext.getLevel(),pContext.getClickedPos().relative(west),blockstate))
                .setValue(INPUT_W,isInputSouth(pContext.getLevel(),pContext.getClickedPos().relative(south),blockstate))
                .setValue(POWERED,false);
    }
    public BlockState rotate(BlockState pState, Rotation pRotation){
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror){
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder){
        pBuilder.add(FACING,INPUT_N,INPUT_E,INPUT_W,INPUT_S,POWERED);
    }

    // Redstone

    public boolean isInputNorth(LevelReader pLevel, BlockPos pPos, BlockState pState){
        Direction north = pState.getValue(FACING);
        return getInputSignalAt(pLevel, pPos, north) > 0;
    }

    public boolean isInputEast(LevelReader pLevel, BlockPos pPos, BlockState pState){
        Direction east = pState.getValue(FACING).getClockWise();
        return getInputSignalAt(pLevel, pPos, east) > 0;
    }

    public boolean isInputWest(LevelReader pLevel, BlockPos pPos, BlockState pState){
        Direction west = pState.getValue(FACING).getCounterClockWise();
        return getInputSignalAt(pLevel, pPos, west) > 0;
    }


    public boolean isInputSouth(LevelReader pLevel, BlockPos pPos, BlockState pState){
        Direction south = pState.getValue(FACING).getOpposite();
        return getInputSignalAt(pLevel, pPos, south) > 0;
    }
    protected int getInputSignalAt(LevelReader pLevel, BlockPos pPos, Direction pSide) {
        BlockState blockstate = pLevel.getBlockState(pPos);

        if (this.isSideInput(blockstate)) {
            if (blockstate.is(Blocks.REDSTONE_BLOCK)) {
                return 15;
            } else {
                return blockstate.is(Blocks.REDSTONE_WIRE) ? blockstate.getValue(RedStoneWireBlock.POWER) : blockstate.getSignal(pLevel, pPos, pSide);
            }
        } else {
            return 0;
        }
    }

    protected boolean isSideInput(BlockState pState) {
        return pState.isSignalSource();
    }
    public boolean isSignalSource(BlockState pState) {
        return true;
    }
    protected int getOutputSignal(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return 15;
    }


    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            pLevel.neighborChanged(pPos.relative(pState.getValue(FACING)),this, pPos);
        }
    }

    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, Random pRand) {
        boolean flag = pState.getValue(POWERED);
        boolean flag1 = this.shouldTurnOn(pLevel, pPos, pState);
        if (flag && !flag1) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(false)), 2);
        } else if(flag1){
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(true)), 2);
        }
        pLevel.neighborChanged(pPos.relative(pState.getValue(FACING)),this,pPos);
    }

    protected boolean shouldTurnOn(Level pLevel, BlockPos pPos, BlockState pState)
    {
        boolean iN = pState.getValue(INPUT_N);
        boolean iE = pState.getValue(INPUT_E);
        boolean iW = pState.getValue(INPUT_W);
        boolean iS = pState.getValue(INPUT_S);

        return (iN && !iE && !iW && !iS) || (!iN && iE && !iW && !iS) || (!iN && !iE && iW && !iS) || (!iN && !iE && !iW && iS)
                ||
                (iN && iE && !iW && !iS) || (iN && !iE && iW && !iS) || (!iN && iE && !iW && iS) || (!iN && !iE && iW && iS);

    }
    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if(!pState.canSurvive(pLevel,pPos)){
            BlockEntity blockentity = pState.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
            dropResources(pState, pLevel, pPos, blockentity);
            pLevel.removeBlock(pPos, false);
        }
        else {
            Direction north = pLevel.getBlockState(pPos).getValue(FACING);
            Direction west = north.getCounterClockWise();
            Direction south = north.getOpposite();
            Direction east = north.getClockWise();

            BlockState blockstate = pLevel.getBlockState(pPos)
                    .setValue(INPUT_N, pLevel.getSignal(pPos.relative(north), north) > 0)
                    .setValue(INPUT_E, pLevel.getSignal(pPos.relative(east), east) > 0)
                    .setValue(INPUT_W, pLevel.getSignal(pPos.relative(west), west) > 0)
                    .setValue(INPUT_S, pLevel.getSignal(pPos.relative(south), south) > 0);
            pLevel.setBlockAndUpdate(pPos, blockstate);
            pLevel.scheduleTick(pPos, this, 1, TickPriority.VERY_HIGH);
        }
    }

    @Override
    public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getSignal(pBlockAccess, pPos, pSide);
    }

    @Override
    public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        if (!pBlockState.getValue(POWERED)) {
            return 0;
        } else {
            return pBlockState.getValue(FACING).getOpposite() == pSide ? this.getOutputSignal(pBlockAccess, pPos, pBlockState) : 0;
        }
    }

    // Block Drops

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {

        List<ItemStack> drops = super.getDrops(pState, pBuilder);
        if (!drops.isEmpty())
            return drops;
        return singletonList(new ItemStack(this, 1));
    }



}