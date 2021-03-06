package de.impelon.disenchanter.blocks;

import java.util.Random;

import de.impelon.disenchanter.DisenchanterMain;
import de.impelon.disenchanter.proxies.CommonProxy;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public class ContainerDisenchantment extends Container {
	
	private World worldObj;
	private int posX;
	private int posY;
	private int posZ;
	private TileEntityDisenchantmentTableAutomatic tileentity;
	private Random random = new Random();
	private IInventory slots = new InventoryBasic("Disenchant", true, 3) {

		@Override
		public int getInventoryStackLimit() {
			return 1;
		}
				
		@Override
		public void markDirty() {
			super.markDirty();
			ContainerDisenchantment.this.onCraftMatrixChanged(this);
		}
	};

	public ContainerDisenchantment(IInventory inventory, World w, int x, int y, int z) {
		this.worldObj = w;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.tileentity = null;
		
		TileEntity te = this.worldObj.getTileEntity(this.posX, this.posY, this.posZ);
		if (te instanceof TileEntityDisenchantmentTableAutomatic) {
			this.tileentity = (TileEntityDisenchantmentTableAutomatic) te;
			this.slots = this.tileentity;
		}

		this.addSlotToContainer(new Slot(this.slots, 0, 26, 35));

		this.addSlotToContainer(new Slot(this.slots, 1, 75, 35) {

			@Override
			public boolean isItemValid(ItemStack stack) {
				return stack.getItem().equals(Items.book);
			}

		});

		this.addSlotToContainer(new Slot(this.slots, 2, 133, 35) {

			@Override
			public boolean isItemValid(ItemStack stack) {
				return false;
			}
			
			@Override
			public void onPickupFromSlot(EntityPlayer p, ItemStack stack) {
				if (tileentity != null)
					return;
				ItemStack itemstack = slots.getStackInSlot(0);
				ItemStack bookstack = slots.getStackInSlot(1);

				if (itemstack != null && bookstack != null) {
					if (bookstack.stackSize > 1)
						bookstack.stackSize -= 1;
					else
						bookstack = (ItemStack) null;
					slots.setInventorySlotContents(1, bookstack);

					int power = 1;
					for (int blockZ = -1; blockZ <= 1; ++blockZ) {
						for (int blockX = -1; blockX <= 1; ++blockX) {
							if ((blockZ != 0 || blockX != 0) && worldObj.isAirBlock(posX + blockX, posY, posZ + blockZ)
									&& worldObj.isAirBlock(posX + blockX, posY + 1, posZ + blockZ)) {
								power += ForgeHooks.getEnchantPower(worldObj, posX + blockX * 2, posY, posZ + blockZ * 2);
								power += ForgeHooks.getEnchantPower(worldObj, posX + blockX * 2, posY + 1, posZ + blockZ * 2);

								if (blockX != 0 && blockZ != 0) {
									power += ForgeHooks.getEnchantPower(worldObj, posX + blockX * 2, posY, posZ + blockZ);
									power += ForgeHooks.getEnchantPower(worldObj, posX + blockX * 2, posY + 1, posZ + blockZ);
									power += ForgeHooks.getEnchantPower(worldObj, posX + blockX, posY, posZ + blockZ * 2);
									power += ForgeHooks.getEnchantPower(worldObj, posX + blockX, posY + 1, posZ + blockZ * 2);
								}
							}
						}
					}
					if (power > 15)
						power = 15;
					int flatDmg = DisenchanterMain.config.get("disenchanting", "FlatDamage", 10).getInt();
					double durabiltyDmg = DisenchanterMain.config.get("disenchanting", "MaxDurabilityDamage", 0.025).getDouble();
					double reduceableDmg = DisenchanterMain.config.get("disenchanting", "MaxDurabilityDamageReduceable", 0.2).getDouble();
					double enchantmentLoss = DisenchanterMain.config.get("disenchanting", "EnchantmentLossChance", 0.0).getDouble();
					itemstack.attemptDamageItem((int) (flatDmg + itemstack.getMaxDamage() * durabiltyDmg + itemstack.getMaxDamage() * (reduceableDmg / power)), random);
					if (itemstack.getItemDamage() > itemstack.getMaxDamage()) {
						slots.setInventorySlotContents(0, (ItemStack) null);
						return;
					}
					if (itemstack != null && itemstack.stackTagCompound != null) {
						NBTTagList enchants = null;
						if (itemstack.stackTagCompound.getTag("ench") != null) {
							enchants = (NBTTagList) itemstack.stackTagCompound.getTag("ench");
							byte loops = 1;
							if (random.nextFloat() <= enchantmentLoss)
								loops = (byte) (1 + random.nextInt(5));
							for (byte n = 0; n < loops; n++) {
								if (enchants.tagCount() > 1)
									enchants.removeTag(0);
								else if (itemstack.stackTagCompound.getTag("ench") != null)
									itemstack.stackTagCompound.removeTag("ench");
							}
						} else if (itemstack.stackTagCompound.getTag("StoredEnchantments") != null) {
							enchants = (NBTTagList) itemstack.stackTagCompound.getTag("StoredEnchantments");
							if (enchants.tagCount() > 1)
								enchants.removeTag(0);
							else
								slots.setInventorySlotContents(0, new ItemStack(Items.book));
						}
					}
				}
			}

		});

		int l;

		for (l = 0; l < 3; ++l)
			for (int i1 = 0; i1 < 9; ++i1)
				this.addSlotToContainer(new Slot(inventory, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));

		for (l = 0; l < 9; ++l)
			this.addSlotToContainer(new Slot(inventory, l, 8 + l * 18, 142));

	}

	@Override
	public void onCraftMatrixChanged(IInventory inventory) {
		super.onCraftMatrixChanged(inventory);

		if (inventory == this.slots && this.tileentity == null)
			this.updateOutput();
	}

	public void updateOutput() {

		if (!this.worldObj.isRemote) {
			ItemStack itemstack = this.slots.getStackInSlot(0);
			ItemStack bookstack = this.slots.getStackInSlot(1);

			if (itemstack != null && bookstack != null
					&& itemstack.stackTagCompound != null) {
				NBTTagList enchants = null;
				if (itemstack.stackTagCompound.getTag("ench") != null)
					enchants = (NBTTagList) itemstack.stackTagCompound.getTag("ench");
				else if (itemstack.stackTagCompound.getTag("StoredEnchantments") != null)
					enchants = (NBTTagList) itemstack.stackTagCompound.getTag("StoredEnchantments");
				else {
					if (this.slots.getStackInSlot(2) != null)
						this.slots.setInventorySlotContents(2, (ItemStack) null);
					return;
				}

				if (enchants.tagCount() > 0) {
					NBTTagCompound enchant = enchants.getCompoundTagAt(0);
					int id = enchant.getInteger("id");
					int lvl = enchant.getInteger("lvl");

					ItemStack outputBookstack = new ItemStack(Items.enchanted_book);
					Items.enchanted_book.addEnchantment(outputBookstack, new EnchantmentData(id, lvl));
					
					if (!(this.slots.getStackInSlot(2) != null && 
							this.slots.getStackInSlot(2).getItem() == Items.enchanted_book && 
							this.slots.getStackInSlot(2).stackTagCompound.getTag("StoredEnchantments").equals(outputBookstack.stackTagCompound.getTag("StoredEnchantments"))))
						this.slots.setInventorySlotContents(2, (ItemStack) outputBookstack);
				}
			} else {
				if (this.slots.getStackInSlot(2) != null)
					this.slots.setInventorySlotContents(2, (ItemStack) null);
			}
		}
	}

	@Override
	public void onContainerClosed(EntityPlayer p) {
		super.onContainerClosed(p);
		
		if (this.tileentity == null) {
			if (!this.worldObj.isRemote) {
				ItemStack itemstack = this.slots.getStackInSlotOnClosing(0);
				ItemStack bookstack = this.slots.getStackInSlotOnClosing(1);
	
				if (itemstack != null)
					p.dropPlayerItemWithRandomChoice(itemstack, false);
				if (bookstack != null)
					p.dropPlayerItemWithRandomChoice(bookstack, false);
			}
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer p) {
		return this.worldObj.getBlock(this.posX, this.posY, this.posZ) != CommonProxy.disenchantmentTable ? false
				: p.getDistanceSq((double) this.posX + 0.5D, (double) this.posY + 0.5D, (double) this.posZ + 0.5D) <= 64.0D;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer p, int slotID) {
		if (p.worldObj.isRemote)
			return null;
		ItemStack itemstackPrev = null;
		ItemStack itemstack = null;
		Slot slot = (Slot) this.inventorySlots.get(slotID);

		if (slot != null && slot.getHasStack()) {
			itemstack = slot.getStack();
			itemstackPrev = itemstack.copy();

			if (slotID == 2) {
				if (!this.mergeItemStack(itemstack, 3, 39, true))
					return null;
				slot.onSlotChange(itemstack, itemstackPrev);
			} else if (slotID != 0 && slotID != 1) {
				ItemStack i = itemstack.splitStack(1);
				if (i.getItem().equals(Items.book)) {
					if (((Slot) this.inventorySlots.get(1)).getHasStack() || !this.mergeItemStack(i, 1, 2, false)) {
						itemstack.stackSize++;
						return null;
					}
				} else {
					if (((Slot) this.inventorySlots.get(0)).getHasStack() || !this.mergeItemStack(i, 0, 1, false)) {
						itemstack.stackSize++;
						return null;
					}
				}
			} else if (!this.mergeItemStack(itemstack, 3, 39, true)) {
				return null;
			}
			
			if (itemstack.stackSize <= 0)
				slot.putStack((ItemStack) null);
			else
				slot.onSlotChanged();

			if (itemstack.stackSize == itemstackPrev.stackSize)
				return null;

			slot.onPickupFromSlot(p, itemstack);
		}

		return itemstack;
	}
}