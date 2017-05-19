using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;
using Microsoft.Win32;

namespace Installer
{
    class Program
    {

        const string URI_SCHEME = "SOFTWARE\\Classes\\.gg";
        const string URI_KEY = "ggp";
        const string URI_KEY_SHELL = "SOFTWARE\\Classes\\ggp";

        static void RegisterUriScheme(string appPath)
        {
            RegistryKey hkeyCurrent = Registry.CurrentUser.CreateSubKey(URI_SCHEME);
            hkeyCurrent.SetValue(null, URI_KEY);
            RegistryKey defaultIcon = hkeyCurrent.CreateSubKey("Default Icon");
            string icontValue = String.Format("\"{0}\",0", appPath);
            defaultIcon.SetValue(null, icontValue);
            RegistryKey hkeyCurrentProt = Registry.CurrentUser.CreateSubKey(URI_KEY_SHELL);
            RegistryKey shell = hkeyCurrentProt.CreateSubKey("shell");
            RegistryKey open = shell.CreateSubKey("open");
            RegistryKey command = open.CreateSubKey("command");
            string cmdValue = String.Format("\"{0}\" \"%1\"", appPath);
            command.SetValue(null, cmdValue);
        }

        static void UnregisterUriScheme()
        {
            Registry.CurrentUser.DeleteSubKeyTree(URI_SCHEME);
            Registry.CurrentUser.DeleteSubKeyTree(URI_KEY_SHELL);
        }
        static void Main(string[] args)
        {
            if ((args.Length > 0) && (args[0].Equals("/u") || args[0].Equals("-u")))
            {
                // uninstall
                Console.Write("Attempting to unregister URI scheme...");

                try
                {
                    UnregisterUriScheme();
                    Console.WriteLine(" Success.");
                }
                catch (Exception ex)
                {
                    Console.WriteLine(" Failed!");
                    Console.WriteLine("{0}: {1}", ex.GetType().Name, ex.Message);
                }
            }
            else {
                // install
                string appPath = Path.Combine(Path.GetDirectoryName(typeof(Program).Assembly.Location), "GnarlyGnat.jar");

                Console.Write("Attempting to register URI scheme...");

                try
                {
                    if (!File.Exists(appPath))
                    {
                        throw new InvalidOperationException(String.Format("Application not found at: {0}", appPath));
                    }

                    RegisterUriScheme(appPath);
                    Console.WriteLine(" Success.");
                }
                catch (Exception ex)
                {
                    Console.WriteLine(" Failed!");
                    Console.WriteLine("{0}: {1}", ex.GetType().Name, ex.Message);
                }
            }

            Console.WriteLine();
            Console.WriteLine("Press any key to continue...");
            Console.ReadKey();
        }
    }
}
