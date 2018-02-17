package com.mrcrayfish.device.programs.email;

import com.mrcrayfish.device.api.ApplicationManager;
import com.mrcrayfish.device.api.app.Application;
import com.mrcrayfish.device.api.app.Component;
import com.mrcrayfish.device.api.app.Dialog;
import com.mrcrayfish.device.api.app.Layout;
import com.mrcrayfish.device.api.app.component.Button;
import com.mrcrayfish.device.api.app.component.Image;
import com.mrcrayfish.device.api.app.component.*;
import com.mrcrayfish.device.api.app.component.Label;
import com.mrcrayfish.device.api.app.component.TextArea;
import com.mrcrayfish.device.api.app.component.TextField;
import com.mrcrayfish.device.api.app.listener.InitListener;
import com.mrcrayfish.device.api.app.renderer.ListItemRenderer;
import com.mrcrayfish.device.api.io.File;
import com.mrcrayfish.device.api.task.TaskManager;
import com.mrcrayfish.device.api.utils.RenderUtil;
import com.mrcrayfish.device.core.Laptop;
import com.mrcrayfish.device.object.AppInfo;
import com.mrcrayfish.device.programs.email.object.Contact;
import com.mrcrayfish.device.programs.email.object.Email;
import com.mrcrayfish.device.programs.email.task.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationEmail extends Application
{
	private static final ResourceLocation ENDER_MAIL_ICONS = new ResourceLocation("cdm:textures/gui/ender_mail.png");

	private static final Pattern EMAIL = Pattern.compile("^([a-zA-Z0-9]{1,10})@endermail\\.com$");
	private final Color COLOR_EMAIL_CONTENT_BACKGROUND = new Color(160, 160, 160);

	/* Loading Layout */
	private Layout layoutInit;
	private Spinner spinnerInit;
	private Label labelLoading;

	/* Main Menu Layout */
	private Layout layoutMainMenu;
	private Image logo;
	private Label labelLogo;
	private Button btnRegisterAccount;

	/* Register Account Layout */
	private Layout layoutRegisterAccount;
	private Label labelEmail;
	private TextField fieldEmail;
	private Label labelDomain;
	private Button btnRegister;

	/* Inbox Layout */
	private Layout layoutInbox;
	private ItemList<Email> listEmails;
	private Button btnViewEmail;
	private Button btnNewEmail;
	private Button btnReplyEmail;
	private Button btnDeleteEmail;
	private Button btnRefresh;

	/* New Email Layout */
	private Layout layoutNewEmail;
	private TextField fieldRecipient;
	private TextField fieldSubject;
	private TextArea textAreaMessage;
	private Button btnSendEmail;
	private Button btnCancelEmail;
	private Button btnAttachedFile;
	private Button btnRemoveAttachedFile;
	private Label labelAttachedFile;

	/* View Email Layout */
	private Layout layoutViewEmail;
	private Label labelViewSubject;
	private Label labelSender;
	private Label labelFrom;
	private Label labelViewSubjectContent;
	private Label labelViewMessage;
	private Text textMessage;
	private Button btnCancelViewEmail;
	private Button btnSaveAttachment;
	private Label labelAttachmentName;
	
	/* Contacts Layout */
	private Layout layoutContacts;
	private ItemList listContacts;
	private Button btnAddContact;
	private Button btnDeleteContact;
	private Button btnCancelContact;
	
	/* Add Contact Layout */
	private Layout layoutAddContact;
	private Label labelContactNickname;
	private TextField fieldContactNickname;
	private Label labelContactEmail;
	private TextField fieldContactEmail;
	private Button btnSaveContact;
	private Button btnCancelAddContact;
	
	/* Insert Contact Layout */
	private Layout layoutInsertContact;
	private ItemList listContacts2;
	private Button btnInsertContact;
	private Button btnCancelInsertContact;

	private String currentName;
	private File attachedFile;
	
	private List<Contact> contacts;

	@Override
	public void init()
	{
		/* Loading Layout */
		layoutInit = new Layout(40, 40);

		spinnerInit = new Spinner(14, 10);
		layoutInit.addComponent(spinnerInit);

		labelLoading = new Label("Loading...", 2, 26);
		layoutInit.addComponent(labelLoading);

		
		/* Main Menu Layout */
		
		layoutMainMenu = new Layout(100, 75);

		logo = new Image(35, 5, 28, 28, info.getIconU(), info.getIconV(), 14, 14, Laptop.ICON_TEXTURES);
		layoutMainMenu.addComponent(logo);

		labelLogo = new Label("Ender Mail", 50, 35);
		labelLogo.setAlignment(Component.ALIGN_CENTER);
		layoutMainMenu.addComponent(labelLogo);

		btnRegisterAccount = new Button(5, 50, "Register");
		btnRegisterAccount.setSize(90, 20);
		btnRegisterAccount.setClickListener((mouseX, mouseY, mouseButton) -> setCurrentLayout(layoutRegisterAccount));
		btnRegisterAccount.setVisible(false);
		layoutMainMenu.addComponent(btnRegisterAccount);

		
		/* Register Account Layout */
		
		layoutRegisterAccount = new Layout(167, 60);

		labelEmail = new Label("Email", 5, 5);
		layoutRegisterAccount.addComponent(labelEmail);

		fieldEmail = new TextField(5, 15, 80);
		layoutRegisterAccount.addComponent(fieldEmail);

		labelDomain = new Label("@endermail.com", 88, 18);
		layoutRegisterAccount.addComponent(labelDomain);

		btnRegister = new Button(5, 35, "Register");
		btnRegister.setSize(157, 20);
		btnRegister.setClickListener((mouseX, mouseY, mouseButton) ->
		{
			int length = fieldEmail.getText().length();
			if(length > 0 && length <= 10)
			{
				TaskRegisterEmailAccount taskRegisterAccount = new TaskRegisterEmailAccount(fieldEmail.getText());
				taskRegisterAccount.setCallback((nbt, success) ->
				{
					if(success)
					{
						currentName = fieldEmail.getText();
						setCurrentLayout(layoutInbox);
					}
					else
					{
						fieldEmail.setTextColor(Color.RED);
					}
				});
				TaskManager.sendTask(taskRegisterAccount);
			}
		});
		layoutRegisterAccount.addComponent(btnRegister);

		
		/* Inbox Layout */
		
		layoutInbox = new Layout(300, 148);
		layoutInbox.setInitListener(new InitListener()
		{
			@Override
			public void onInit()
			{
				TaskUpdateInbox taskUpdateInbox = new TaskUpdateInbox();
				taskUpdateInbox.setCallback((nbt, success) ->
				{
                    listEmails.removeAll();
                    for (Email email : EmailManager.INSTANCE.getInbox())
                    {
                        listEmails.addItem(email);
                    }
                });
				TaskManager.sendTask(taskUpdateInbox);
			}
		});

		listEmails = new ItemList<Email>(5, 25, 275, 4);
		listEmails.setListItemRenderer(new ListItemRenderer<Email>(28)
		{
			@Override
			public void render(Email e, Gui gui, Minecraft mc, int x, int y, int width, int height, boolean selected)
			{
				Gui.drawRect(x, y, x + width, y + height, selected ? Color.DARK_GRAY.getRGB() : Color.GRAY.getRGB());

				if (!e.isRead())
				{
					GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
					mc.getTextureManager().bindTexture(ENDER_MAIL_ICONS);
					gui.drawTexturedModalRect(x + 247, y + 8, 0, 10, 20, 12);
				}

				if(e.getAttachment() != null)
				{
					GlStateManager.color(1.0F, 1.0F, 1.0F);
					int posX = x + (!e.isRead() ? -30 : 0) + 255;
					mc.getTextureManager().bindTexture(ENDER_MAIL_ICONS);
					gui.drawTexturedModalRect(posX, y + 5, 20, 10, 13, 20);
				}

				mc.fontRenderer.drawString(e.getSubject(), x + 5, y + 5, Color.WHITE.getRGB());
				mc.fontRenderer.drawString(e.getAuthor() + "@endermail.com", x + 5, y + 18, Color.LIGHT_GRAY.getRGB());
			}
		});
		layoutInbox.addComponent(listEmails);

		btnViewEmail = new Button(5, 5, ENDER_MAIL_ICONS, 30, 0, 10, 10);
		btnViewEmail.setClickListener((mouseX, mouseY, mouseButton) ->
		{
            int index = listEmails.getSelectedIndex();
            if (index != -1)
            {
                TaskManager.sendTask(new TaskViewEmail(index));
                Email email = listEmails.getSelectedItem();
                email.setRead(true);
                textMessage.setText(email.getMessage());
                labelViewSubject.setText(email.getSubject());
                labelFrom.setText(email.getAuthor() + "@endermail.com");
                attachedFile = email.getAttachment();
                if(attachedFile != null)
                {
                    btnSaveAttachment.setVisible(true);
                    labelAttachmentName.setVisible(true);
                    labelAttachmentName.setText(attachedFile.getName());
                }
                setCurrentLayout(layoutViewEmail);
            }
        });
		btnViewEmail.setToolTip("View", "Opens the currently selected email");
		layoutInbox.addComponent(btnViewEmail);

		btnNewEmail = new Button(25, 5, ENDER_MAIL_ICONS, 0, 0, 10, 10);
		btnNewEmail.setClickListener((mouseX, mouseY, mouseButton) -> setCurrentLayout(layoutNewEmail));
		btnNewEmail.setToolTip("New Email", "Send an email to a player");
		layoutInbox.addComponent(btnNewEmail);

		btnReplyEmail = new Button(45, 5, ENDER_MAIL_ICONS, 60, 0, 10, 10);
		btnReplyEmail.setClickListener((mouseX, mouseY, mouseButton) ->
		{
            Email email = listEmails.getSelectedItem();
            if (email != null)
            {
                setCurrentLayout(layoutNewEmail);
                fieldRecipient.setText(email.getAuthor() + "@endermail.com");
                fieldSubject.setText("RE: " + email.getSubject());
            }
        });
		btnReplyEmail.setToolTip("Reply", "Reply to the currently selected email");
		layoutInbox.addComponent(btnReplyEmail);

		btnDeleteEmail = new Button(65, 5, ENDER_MAIL_ICONS, 10, 0, 10, 10);
		btnDeleteEmail.setClickListener((mouseX, mouseY, mouseButton) ->
		{
			final int index = listEmails.getSelectedIndex();
			if(index != -1)
			{
				TaskDeleteEmail taskDeleteEmail = new TaskDeleteEmail(index);
				taskDeleteEmail.setCallback((nbt, success) ->
				{
					listEmails.removeItem(index);
					EmailManager.INSTANCE.getInbox().remove(index);
				});
				TaskManager.sendTask(taskDeleteEmail);
			}
		});
		btnDeleteEmail.setToolTip("Trash Email", "Deletes the currently select email");
		layoutInbox.addComponent(btnDeleteEmail);

		btnRefresh = new Button(85, 5, ENDER_MAIL_ICONS, 20, 0, 10, 10);
		btnRefresh.setClickListener((mouseX, mouseY, mouseButton) ->
		{
			TaskUpdateInbox taskUpdateInbox = new TaskUpdateInbox();
			taskUpdateInbox.setCallback((nbt, success) ->
			{
				listEmails.removeAll();
				for(Email email : EmailManager.INSTANCE.getInbox())
				{
					listEmails.addItem(email);
				}
			});
			TaskManager.sendTask(taskUpdateInbox);
		});
		btnRefresh.setToolTip("Refresh Inbox", "Checks for any new emails");
		layoutInbox.addComponent(btnRefresh);

		
		/* New Email Layout */
		
		layoutNewEmail = new Layout(231, 148);
		layoutNewEmail.setBackground((gui, mc, x, y, width, height, mouseX, mouseY, windowActive) ->
		{
			if(attachedFile != null)
			{
				AppInfo info = ApplicationManager.getApplication(attachedFile.getOpeningApp());
				RenderUtil.drawApplicationIcon(info, x + 46, y + 130);
			}
        });

		fieldRecipient = new TextField(26, 5, 200);
		fieldRecipient.setPlaceholder("To");
		layoutNewEmail.addComponent(fieldRecipient);

		fieldSubject = new TextField(26, 23, 200);
		fieldSubject.setPlaceholder("Subject");
		layoutNewEmail.addComponent(fieldSubject);

		textAreaMessage = new TextArea(26, 41, 200, 85);
		textAreaMessage.setPlaceholder("Message");
		layoutNewEmail.addComponent(textAreaMessage);

		btnSendEmail = new Button(5, 5, ENDER_MAIL_ICONS, 50, 0, 10, 10);
		btnSendEmail.setClickListener((mouseX, mouseY, mouseButton) ->
		{
			Matcher matcher = EMAIL.matcher(fieldRecipient.getText());
			if(!matcher.matches()) return;

			Email email = new Email(fieldSubject.getText(), textAreaMessage.getText(), attachedFile);
			TaskSendEmail taskSendEmail = new TaskSendEmail(email, matcher.group(1));
			taskSendEmail.setCallback((nbt, success) ->
			{
				if(success)
				{
					setCurrentLayout(layoutInbox);
					textAreaMessage.clear();
					fieldSubject.clear();
					fieldRecipient.clear();
					resetAttachedFile();
				}
			});
			TaskManager.sendTask(taskSendEmail);
		});
		btnSendEmail.setToolTip("Send", "Send email to recipient");
		layoutNewEmail.addComponent(btnSendEmail);

		btnCancelEmail = new Button(5, 25, ENDER_MAIL_ICONS, 40, 0, 10, 10);
		btnCancelEmail.setClickListener((mouseX, mouseY, mouseButton) ->
		{
            setCurrentLayout(layoutInbox);
            textAreaMessage.clear();
            fieldSubject.clear();
            fieldRecipient.clear();
            resetAttachedFile();
        });
		btnCancelEmail.setToolTip("Cancel", "Go back to Inbox");
		layoutNewEmail.addComponent(btnCancelEmail);

		btnAttachedFile = new Button(26, 129, ENDER_MAIL_ICONS, 70, 0, 10, 10);
		btnAttachedFile.setToolTip("Attach File", "Select a file from computer to attach to this email");
		btnAttachedFile.setClickListener((mouseX, mouseY, mouseButton) ->
		{
            if(mouseButton == 0)
			{
				Dialog.OpenFile dialog = new Dialog.OpenFile(this);
				dialog.setResponseHandler((success, file) ->
				{
					if(!file.isFolder())
					{
						attachedFile = file.copy();
						labelAttachedFile.setText(file.getName());
						labelAttachedFile.left += 16;
						labelAttachedFile.xPosition += 16;
						btnAttachedFile.setVisible(false);
						btnRemoveAttachedFile.setVisible(true);
						dialog.close();
					}
					else
					{
						openDialog(new Dialog.Message("Attachment must be a file!"));
					}
					return false;
				});
				openDialog(dialog);
			}
        });
		layoutNewEmail.addComponent(btnAttachedFile);

		btnRemoveAttachedFile = new Button(26, 129, ENDER_MAIL_ICONS, 40, 0, 10, 10);
		btnRemoveAttachedFile.setToolTip("Remove Attachment", "Delete the attached file from this email");
		btnRemoveAttachedFile.setVisible(false);
		btnRemoveAttachedFile.setClickListener((mouseX, mouseY, mouseButton) ->
		{
            if(mouseButton == 0)
			{
				resetAttachedFile();
			}
        });
		layoutNewEmail.addComponent(btnRemoveAttachedFile);

		labelAttachedFile = new Label("No file attached", 46, 133);
		layoutNewEmail.addComponent(labelAttachedFile);
		
		
		/* View Email Layout */
		
		layoutViewEmail = new Layout(240, 156);
		layoutViewEmail.setBackground((gui, mc, x, y, width, height, mouseX, mouseY, windowActive) ->
		{
            Gui.drawRect(x, y + 22, x + layoutViewEmail.width, y + 50, Color.GRAY.getRGB());
            Gui.drawRect(x, y + 22, x + layoutViewEmail.width, y + 23, Color.DARK_GRAY.getRGB());
            Gui.drawRect(x, y + 49, x + layoutViewEmail.width, y + 50, Color.DARK_GRAY.getRGB());
            Gui.drawRect(x, y + 50, x + layoutViewEmail.width, y + 156, COLOR_EMAIL_CONTENT_BACKGROUND.getRGB());

			if(attachedFile != null)
			{
				GlStateManager.color(1.0F, 1.0F, 1.0F);
				AppInfo info = ApplicationManager.getApplication(attachedFile.getOpeningApp());
				RenderUtil.drawApplicationIcon(info, x + 204, y + 4);
			}
		});

		labelViewSubject = new Label("Subject", 5, 26);
		labelViewSubject.setTextColor(new Color(255, 170, 0));
		layoutViewEmail.addComponent(labelViewSubject);

		labelFrom = new Label("From", 5, 38);
		layoutViewEmail.addComponent(labelFrom);

		btnCancelViewEmail = new Button(5, 3, ENDER_MAIL_ICONS, 40, 0, 10, 10);
		btnCancelViewEmail.setClickListener((mouseX, mouseY, mouseButton) ->
		{
			if(mouseButton == 0)
			{
				attachedFile = null;
				btnSaveAttachment.setVisible(false);
				labelAttachmentName.setVisible(false);
				setCurrentLayout(layoutInbox);
			}

		});
		btnCancelViewEmail.setToolTip("Cancel", "Go back to Inbox");
		layoutViewEmail.addComponent(btnCancelViewEmail);

		textMessage = new Text("Hallo", 5, 54, 230);
		textMessage.setShadow(false);
		layoutViewEmail.addComponent(textMessage);

		btnSaveAttachment = new Button(219, 3, ENDER_MAIL_ICONS, 80, 0, 10, 10);
		btnSaveAttachment.setToolTip("Save Attachment", "Save the file attached to this email");
		btnSaveAttachment.setVisible(false);
		btnSaveAttachment.setClickListener((mouseX, mouseY, mouseButton) ->
		{
            if(mouseButton == 0 && attachedFile != null)
            {
            	Dialog.SaveFile dialog = new Dialog.SaveFile(this, attachedFile);
            	openDialog(dialog);
			}
        });
		layoutViewEmail.addComponent(btnSaveAttachment);

		labelAttachmentName = new Label("", 200, 7);
		labelAttachmentName.setVisible(false);
		labelAttachmentName.setAlignment(Component.ALIGN_RIGHT);
		layoutViewEmail.addComponent(labelAttachmentName);

		setCurrentLayout(layoutInit);

		TaskCheckEmailAccount taskCheckAccount = new TaskCheckEmailAccount();
		taskCheckAccount.setCallback((nbt, success) ->
		{
            if (success)
            {
                currentName = nbt.getString("Name");
                listEmails.removeAll();
                for (Email email : EmailManager.INSTANCE.getInbox())
                {
                    listEmails.addItem(email);
                }
                setCurrentLayout(layoutInbox);
            }
            else
            {
                btnRegisterAccount.setVisible(true);
                setCurrentLayout(layoutMainMenu);
            }
        });
		TaskManager.sendTask(taskCheckAccount);
	}

	private void resetAttachedFile()
	{
		if(attachedFile != null)
		{
			labelAttachedFile.setText("No file attached");
			labelAttachedFile.left -= 16;
			labelAttachedFile.xPosition -= 16;
			btnRemoveAttachedFile.setVisible(false);
			btnAttachedFile.setVisible(true);
			attachedFile = null;
		}
	}

	@Override
	public void load(NBTTagCompound tagCompound)
	{
		
	}

	@Override
	public void save(NBTTagCompound tagCompound)
	{
		
	}

	@Override
	public String getWindowTitle()
	{
		if (getCurrentLayout() == layoutInbox)
		{
			return "Inbox: " + currentName + "@endermail.com";
		}
		if(getCurrentLayout() == layoutContacts)
		{
			return "Contacts";
		}
		return info.getName();
	}

	@Override
	public void onClose()
	{
		super.onClose();
		attachedFile = null;
	}
}
