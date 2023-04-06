package com.critmxbelvix.simplyrs.common.blocks;

import com.critmxbelvix.simplyrs.common.blocks.entities.ArithmeticUnit.ArithmeticBlockEntity;
import com.critmxbelvix.simplyrs.common.blocks.srsvoxelshapes.SRSVoxelShapes;
import com.critmxbelvix.simplyrs.common.creativetabs.SimplyRSCreativeTab;
import com.critmxbelvix.simplyrs.common.items.RedstoneWrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static java.util.Collections.singletonList;

public class RedstoneArithmeticUnit extends Block implements EntityBlock {
    private static final Logger LOGGER = LogManager.getLogger();
    final static String name = "redstone_arithmetic_unit";
    final static CreativeModeTab tab = SimplyRSCreativeTab.SRS_TAB;
    final static Properties arithmetic_unit_properties = Properties.of(Material.STONE).strength(0.3f).dynamicShape();
    public static final EnumProperty<ArithmeticModes> MODE = EnumProperty.create("mode", ArithmeticModes.class, ArithmeticModes.values());
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public RedstoneArithmeticUnit(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(MODE, ArithmeticModes.ADD)
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
        return arithmetic_unit_properties;
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canSupportRigidBlock(pLevel, pPos.below());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SRSVoxelShapes.ARITHMETIC_UNIT_SHAPE;
    }

    // Blockstates

    public BlockState getStateForPlacement(BlockPlaceContext pContext)
    {
        BlockState blockstate = super.getStateForPlacement(pContext);
        Level pLevel = pContext.getLevel();
        BlockPos pPos = pContext.getClickedPos();
        Direction direction = pContext.getHorizontalDirection();
        pContext.getLevel().scheduleTick(pContext.getClickedPos(),blockstate.getBlock(),1);
        pLevel.updateNeighborsAt(pPos.relative(direction),blockstate.getBlock());

        return this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(MODE, ArithmeticModes.ADD);
    }


    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder){
        pBuilder.add(FACING, MODE);
    }

    public BlockState rotate(BlockState pState, Rotation pRotation){
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror){
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
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

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(pPlayer.getItemInHand(pHand).getItem() instanceof RedstoneWrench){
            ArithmeticModes mode = pState.getValue(MODE);
            ArithmeticModes newMode;
            newMode = switch(mode){
                case ADD -> ArithmeticModes.SUBTRACT;
                case SUBTRACT -> ArithmeticModes.MULTIPLY;
                case MULTIPLY -> ArithmeticModes.DIVIDE;
                case DIVIDE -> ArithmeticModes.ADD;
            };
            pLevel.setBlockAndUpdate(pPos,pState.setValue(MODE,newMode));
            pLevel.updateNeighborsAt(pPos.relative(pState.getValue(FACING)),pState.getBlock());
        }
        else if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if(entity instanceof ArithmeticBlockEntity) {
                NetworkHooks.openGui(((ServerPlayer)pPlayer), (ArithmeticBlockEntity)entity, pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ArithmeticBlockEntity(pPos,pState);
    }

    public enum ArithmeticModes implements StringRepresentable {
        ADD("add"),
        SUBTRACT("subtract"),
        MULTIPLY("multiply"),
        DIVIDE("divide");

        private final String name;

        ArithmeticModes(String pName){
            this.name = pName;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    // Redstone

    public boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(FACING).getOpposite() == pSide ? this.getOutputSignal(pBlockAccess, pPos, pBlockState) : 0;
    }
    @Override
    public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getSignal(pBlockAccess, pPos, pSide);
    }

    protected int getInputSignalAt(BlockGetter pLevel, BlockPos pPos, Direction pSide) {
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

    protected int getOutputSignal(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        return blockentity instanceof ArithmeticBlockEntity ? ((ArithmeticBlockEntity)blockentity).getOutputSignal() : 0;
    }

    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, Random pRand) {
        pLevel.neighborChanged(pPos.relative(pState.getValue(FACING)),this,pPos);
    }

    private int addRedstone(int a, int b, int c){
        if(a==-1){
            a=0;
        }
        if(b==-1){
            b=0;
        }
        if(c==-1){
            c=0;
        }
        return Math.min((a + b + c), 15);
    }

    private int subtractRedstone(int a, int b, int c){
        if(a==-1){
            a=0;
        }
        if(b==-1){
            b=0;
        }
        if(c==-1){
            c=0;
        }
        return (a - b - c) >= 0 ? (a - b - c) : -(a - b - c);
    }

    private int divideRedstone(int a, int b, int c) {
        if(a==-1 && b!=0 && c!=0){
            return b/c;
        }
        else if(b==-1 && a!=0 && c!=0){
            return a/c;
        }
        else if(c==-1 && a!=0 && b!=0){
            return a/b;
        }
        return a!=0 && b!=0 && c!=0 ? a/b/c : 0;
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {

        Direction north = pState.getValue(FACING);
        Direction south = north.getOpposite();
        Direction east = north.getClockWise();
        Direction west = north.getCounterClockWise();

        if(!pState.canSurvive(pLevel,pPos)){
            BlockEntity blockentity = pState.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
            dropResources(pState, pLevel, pPos, blockentity);
            pLevel.removeBlock(pPos, false);
        }
        else {
            ArithmeticModes mode = pState.getValue(MODE);
            Direction direction = pState.getValue(FACING);
            BlockEntity be = pLevel.getBlockEntity(pPos);
            Direction aDir= west;
            Direction bDir = south;
            Direction cDir = east;
            int operand1, operand2, operand3;
            int operands=-1;

            int a,b,c;

            if(be instanceof  ArithmeticBlockEntity) {
                operands = ((ArithmeticBlockEntity) be).getOperands();
                operand1 = operands & 0b11;
                operand2 = operands & 0b1100;
                operand2 = operand2 >> 2;
                operand3 = operands & 0b110000;
                operand3 = operand3 >> 4;
                switch(operand1){
                    case 0:
                        aDir = west;
                        break;
                    case 1:
                        aDir = south;
                        break;
                    case 2:
                        aDir = east;
                        break;
                    case 3:
                        aDir = Direction.UP;
                        break;
                }
                switch(operand2){
                    case 0:
                        bDir = west;
                        break;
                    case 1:
                        bDir = south;
                        break;
                    case 2:
                        bDir = east;
                        break;
                    case 3:
                        bDir = Direction.UP;
                        break;
                }
                switch(operand3){
                    case 0:
                        cDir = west;
                        break;
                    case 1:
                        cDir = south;
                        break;
                    case 2:
                        cDir = east;
                        break;
                    case 3:
                        cDir = Direction.UP;
                        break;
                }
            }

            if(aDir!=Direction.UP){
                a = getInputSignalAt(pLevel,pPos.relative(aDir),aDir);
            }
            else{
                a=-1;
            }
            if(bDir!=Direction.UP){
                b = getInputSignalAt(pLevel,pPos.relative(bDir),bDir);
            }
            else{
                b=-1;
            }
            if(cDir!=Direction.UP){
                c = getInputSignalAt(pLevel,pPos.relative(cDir),cDir);
            }
            else{
                c=-1;
            }

            int strength = switch (mode) {
                case ADD -> addRedstone(a,b,c);

                case SUBTRACT -> subtractRedstone(a,b,c);

                case MULTIPLY -> Math.min((a * b * c), 15);

                case DIVIDE -> divideRedstone(a,b,c);
            };
            if(be instanceof  ArithmeticBlockEntity) {
                ((ArithmeticBlockEntity) pLevel.getBlockEntity(pPos)).setOutputSignal(strength);
            }
            pLevel.scheduleTick(pPos, this, 1, TickPriority.VERY_HIGH);
        }
    }



    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            pLevel.neighborChanged(pPos.relative(pState.getValue(FACING)),this, pPos);
        }
    }


}
