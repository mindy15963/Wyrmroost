package WolfShotz.Wyrmroost.network.messages;

import WolfShotz.Wyrmroost.items.staff.DragonStaffItem;
import WolfShotz.Wyrmroost.items.staff.StaffAction;
import WolfShotz.Wyrmroost.network.IMessage;
import WolfShotz.Wyrmroost.registry.WRItems;
import WolfShotz.Wyrmroost.util.ModUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class StaffActionMessage implements IMessage
{
    public final StaffAction action;

    public StaffActionMessage(StaffAction action) { this.action = action; }

    public StaffActionMessage(PacketBuffer buf) { action = StaffAction.VALUES[buf.readInt()]; }

    @Override
    public void encode(PacketBuffer buf) { buf.writeInt(action.ordinal()); }

    @Override
    public void run(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayerEntity player = context.get().getSender();
        if (player == null) return;
        ItemStack stack = ModUtils.getHeldStack(context.get().getSender(), WRItems.DRAGON_STAFF.get());
        if (stack.getItem() == WRItems.DRAGON_STAFF.get())
            DragonStaffItem.setAction(action, player, stack);
    }
}
