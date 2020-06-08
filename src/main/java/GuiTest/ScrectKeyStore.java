package GuiTest;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import lombok.SneakyThrows;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.crypto.keystore.KeyStoreImpl;
import org.tdf.crypto.keystore.SMKeystore;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.sunflower.facade.SecretStoreImpl;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.util.MappingUtil;

public class ScrectKeyStore extends JFrame implements ActionListener{

    static {
        CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
        CryptoContext.setSigner((sk, msg) -> new SM2PrivateKey(sk).sign(msg));
        CryptoContext.setSecretKeyGenerator(() -> SM2.generateKeyPair().getPrivateKey().getEncoded());
        CryptoContext.setGetPkFromSk((sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded());
        CryptoContext.setEcdh((initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID));
        CryptoContext.setEncrypt(CryptoHelpers.ENCRYPT);
        CryptoContext.setDecrypt(CryptoHelpers.DECRYPT);
    }

    public static void main(String[] args) {
        new ScrectKeyStore();
    }

    private static final long serialVersionUID = -1189035634361220261L;
    String pwd;
    KeyStoreImpl k;
    JFrame mainframe;
    JTabbedPane tabbedPane = new JTabbedPane();
    ImageIcon icon = createImageIcon();
    JPanel panel_for_create_secret_store;
    JPanel panel_for_create_key_store;
    //创建相关的Label标签
    JLabel pubkey_label = new JLabel("pubkey:");
    JLabel password_label = new JLabel("password:");
    JLabel private_key_label = new JLabel("skkey:");
    //创建相关的文本域
    JTextField pubkey_textfield = new JTextField(20);
    JTextField password_textfield = new JTextField(20);
    JTextField private_key_textfield = new JTextField(20);
    //创建按钮
    JButton start_button_for_create_secret_store = new JButton("CREATE SECRET STORE");
    JButton start_button_for_create_key_store = new JButton("CREATE KEY STORE");

    public ScrectKeyStore() {
        mainframe = new JFrame("小工具");
        mainframe.setSize(575, 500);
        mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainframe.setResizable(false);

        Toolkit kit = Toolkit.getDefaultToolkit(); // 定义工具包
        Dimension screenSize = kit.getScreenSize(); // 获取屏幕的尺寸
        int screenWidth = screenSize.width/2; // 获取屏幕的宽
        int screenHeight = screenSize.height/2; // 获取屏幕的高
        int height = mainframe.getHeight(); //获取窗口高度
        int width = mainframe.getWidth(); //获取窗口宽度
        mainframe.setLocation(screenWidth-width/2, screenHeight-height/2);//将窗口设置到屏幕的中部
        //窗体居中，c是Component类的父窗口
        //mainframe.setLocationRelativeTo(c);
        initPanel();//初始化面板
        panel_for_create_key_store = makeTextPanel();
        tabbedPane.addTab("create key store", icon, panel_for_create_key_store,"create key store");
        tabbedPane.addTab("create secret store", icon, panel_for_create_secret_store,"create secret store");
        mainframe.add(this.tabbedPane, BorderLayout.CENTER);
        mainframe.setVisible(true);
    }

    protected JPanel makeTextPanel() {
        JPanel panel  = new JPanel();
        panel.setLayout(null);
        password_label.setBounds(10,20,80,25);
        password_textfield.setBounds(100,20,400,25);
        panel.add(password_label);
        panel.add(password_textfield);
        private_key_label.setBounds(10,50,80,25);
        private_key_textfield.setBounds(100,50,400,25);
        panel.add(private_key_label);
        panel.add(private_key_textfield);
        start_button_for_create_key_store.setBounds(180,120,220,25);
        panel.add(start_button_for_create_key_store);
        //增加动作监听
        start_button_for_create_key_store.addActionListener(this);
        return panel;
    }

    public void initPanel(){
        panel_for_create_secret_store = new JPanel();
        panel_for_create_secret_store.setLayout(null);
        //this.panel = new JPanel(new GridLayout(3,2)); //创建3行3列的容器
        /* 这个方法定义了组件的位置。
         * setBounds(x, y, width, height)
         * x和y指定左上角的新位置，由width和height指定新的大小。
         */
        pubkey_label.setBounds(10,20,80,25);
        pubkey_textfield.setBounds(100,20,400,25);
        this.panel_for_create_secret_store.add(pubkey_label);
        this.panel_for_create_secret_store.add(pubkey_textfield);

        start_button_for_create_secret_store.setBounds(180,70,220,25);
        this.panel_for_create_secret_store.add(start_button_for_create_secret_store);
        //增加动作监听
        start_button_for_create_secret_store.addActionListener(this);
    }

    @Override
    @SneakyThrows
    public void actionPerformed(ActionEvent event) {
        if(event.getSource() == start_button_for_create_secret_store)
        {
            String pubkeyStr = pubkey_textfield.getText();
//            pubkeyStr = "03cac34009c85674f46f0801d195a216030807f6aa2be337e754ae7645bf7a1106";
            System.out.println(pubkeyStr);
            byte[] aliceSk = CryptoContext.generateSecretKey();
            byte[] alicePk = CryptoContext.getPkFromSk(aliceSk);
            byte[] bobPk = HexBytes.decode(pubkeyStr);
            byte[] key = CryptoContext.ecdh(true, aliceSk, bobPk);
            byte[] plain = SMKeystore.decryptKeyStore(k, pwd);
            byte[] cipher = CryptoContext.encrypt(key, plain);
            SecretStoreImpl s = new SecretStoreImpl(
                    HexBytes.fromBytes(alicePk),
                    HexBytes.fromBytes(cipher)
            );
            try(FileWriter file = new FileWriter("secret_store.jsonc"))
            {
                file.write(MappingUtil.OBJECT_MAPPER.writeValueAsString(s));
                file.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else if(event.getSource() == start_button_for_create_key_store)
        {
            pwd = password_textfield.getText();
            String sk = private_key_textfield.getText();
            System.out.println(pwd);
            System.out.println(sk);
            byte[] privateKey = sk.getBytes();
            k = SMKeystore.generateKeyStore(pwd, privateKey);
            try(FileWriter file = new FileWriter("key_store.jsonc"))
            {
                file.write(MappingUtil.OBJECT_MAPPER.writeValueAsString(k));
                file.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void windowClosed(WindowEvent arg0) {
        System.exit(0);
    }

    public void windowClosing(WindowEvent arg0) {
        System.exit(0);
    }

    protected static ImageIcon createImageIcon() {
        String path = "123.gif";
        java.net.URL imgURL = ClassLoader.getSystemClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public static String getFileContents(String fileName) throws Exception {
        File theFile = new File(fileName);
        byte[] bytes = new byte[(int) theFile.length()];
        InputStream in = new FileInputStream(theFile);
        int m = 0, n = 0;
        while (m < bytes.length) {
            n = in.read(bytes, m, bytes.length - m);
            m += n;
        }

        in.close();
        return new String(bytes);
    }

    public void saveFile() {
        //弹出文件选择框
        JFileChooser chooser = new JFileChooser();

        //后缀名过滤器
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "通讯录文件(*.con)", "con");
        chooser.setFileFilter(filter);

        //下面的方法将阻塞，直到【用户按下保存按钮且“文件名”文本框不为空】或【用户按下取消按钮】
        int option = chooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {    //假如用户选择了保存
            File file = chooser.getSelectedFile();

            String fname = chooser.getName(file);    //从文件名输入框中获取文件名

            //假如用户填写的文件名不带我们制定的后缀名，那么我们给它添上后缀
            if (!fname.endsWith(".con")) {
                file = new File(chooser.getCurrentDirectory(), fname + ".con");
                System.out.println("renamed");
                System.out.println(file.getName());
            }

            try {
                FileOutputStream fos = new FileOutputStream(file);

                //写文件操作……

                fos.close();

            } catch (IOException e) {
                System.err.println("IO异常");
                e.printStackTrace();
            }
        }
    }

     /**
     * 读取json文件，返回json串
     * @param fileName
     * @return
     */
    public static String readJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
