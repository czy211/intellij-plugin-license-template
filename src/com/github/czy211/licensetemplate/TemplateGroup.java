package com.github.czy211.licensetemplate;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.time.LocalDate;
import java.util.Objects;

public class TemplateGroup extends ActionGroup {

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        String[] types = {"GNU AGPLv3", "GNU GPLv3", "GNU LGPLv3", "Mozilla Public License 2.0", "Apache License 2.0",
                "MIT License", "The Unlicense", "CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0", "SIL 开源字体协议"};
        Icon icon = AllIcons.FileTypes.Text;

        // 设置ActionGroup的图标
        Objects.requireNonNull(anActionEvent).getPresentation().setIcon(icon);
        // 创建AnAction数组，对应不同协议类型的LICENSE
        AnAction[] actions = new AnAction[types.length];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new Template(types[i], "创建" + types[i] + "协议的LICENSE", icon);
        }
        return actions;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 当前路径中存在license文件时，隐藏此Action
        e.getPresentation().setEnabledAndVisible(!isExist(e.getDataContext()));
    }

    /**
     * 创建LICENSE文件
     *
     * @param fileName 当前弹出右键菜单的文件
     * @param content LICENSE文件内容
     * @return LICENSE文件路径
     */
    private String createFile(String fileName, String content) {
        String path;
        File file = new File(fileName);
        // file是目录时在当前路径下创建LICENSE文件，是文件时，在其父目录下创建LICENSE文件
        path = (file.isDirectory() ? fileName : file.getParent()) + "/LICENSE";
        try (PrintWriter output = new PrintWriter(path)) {
            output.print(content);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return path;
    }

    /**
     * 替换模板中的{$YEAR}和{$NAME}
     *
     * @param tempContext 模板文件内容
     * @return LICENSE文件内容
     */
    private String replaceTemplate(String tempContext) {
        int year = LocalDate.now().getYear();
        String name = System.getProperty("user.name");
        return tempContext.replace("{$YEAR}", year + "").replace("{$NAME}", name);
    }

    /**
     * 从模板文件中读取内容
     *
     * @param fileName 模板文件路径
     * @return 模板文件内容
     */
    private String readFromTemplate(String fileName) {
        try {
            InputStream in = Objects.requireNonNull(this.getClass().getClassLoader().getResource(fileName)).openStream();
            return StreamUtil.readText(in, "utf-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 检查当前路径中是否存在LICENSE文件
     *
     * @param dataContext dataContext
     * @return boolean
     */
    private boolean isExist(DataContext dataContext) {
        VirtualFile file = PlatformDataKeys.VIRTUAL_FILE.getData(dataContext);
        VirtualFile[] files = Objects.requireNonNull(file).isDirectory() ? file.getChildren() : file.getParent()
                .getChildren();
        for (VirtualFile f : files) {
            if ("license".equalsIgnoreCase(f.getNameWithoutExtension())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 定义Action类
     */
    class Template extends AnAction {
        Template(@Nls(capitalization = Nls.Capitalization.Title) @Nullable String text,
                 @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description, @Nullable Icon icon) {
            super(text, description, icon);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            String tempContext = readFromTemplate(this.getTemplateText());
            String content = replaceTemplate(tempContext);
            DataContext dataContext = anActionEvent.getDataContext();
            String fileName = Objects.requireNonNull(PlatformDataKeys.VIRTUAL_FILE.getData(dataContext)).getPath();
            String path = createFile(fileName, content);
            Project project = Objects.requireNonNull(anActionEvent.getProject());
            // 打开LICENSE文件
            OpenFileAction.openFile(path, project);
        }
    }
}