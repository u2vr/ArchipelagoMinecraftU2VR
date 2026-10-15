import json
import zipfile
from base64 import b64encode
from typing import Any, Optional

from worlds.Files import APPlayerContainer


class MinecraftContainer(APPlayerContainer):
    """
    Generates the apmc file
    """
    game = "Minecraft with BACAP"
    patch_file_ending = ".apmc"

    def __init__(self,
                 patch_data: Any = None,
                 patch_name: str = "",
                 path: Optional[str] = None,
                 player: Optional[int] = None,
                 player_name: str = "",
                 server: str = "",
                 password: str = ""):
        # Handle case where AutoPatchRegister instantiates with (path, player, player_name, server)
        if isinstance(patch_data, str) and (isinstance(patch_name, int) or player is None):
            server = player_name
            player_name = str(path) if path is not None else ""
            player = patch_name
            path = patch_data
            patch_data = {}
            patch_name = ""

        super().__init__(path, player, player_name, server)
        self.patch_data = patch_data if isinstance(patch_data, dict) else {}
        self.patch_name = patch_name or (player_name if player_name else "patch")
        self.password = password

        if password and "password" not in self.patch_data:
            self.patch_data["password"] = password
        if server:
            self._apply_server(server)

    def _apply_server(self, server_str: str) -> None:
        if not server_str:
            return
        server_str = str(server_str).strip()
        self.server = server_str
        if ":" in server_str:
            host, port_str = server_str.rsplit(":", 1)
            try:
                self.patch_data["server"] = host
                self.patch_data["port"] = int(port_str)
            except ValueError:
                self.patch_data["server"] = server_str
        else:
            self.patch_data["server"] = server_str

    def get_manifest(self) -> dict[str, Any]:
        manifest = super().get_manifest() if hasattr(super(), "get_manifest") else {}
        if self.password:
            manifest["password"] = self.password
        if self.server:
            manifest["server"] = self.server
        return manifest

    def read_contents(self, opened_zipfile: zipfile.ZipFile) -> None:
        super().read_contents(opened_zipfile)
        for entry in opened_zipfile.infolist():
            if entry.filename.endswith(".apmcmeta") or entry.filename.endswith(".apmc"):
                meta_bytes = opened_zipfile.read(entry)
                self.patch_data = json.loads(meta_bytes.decode("utf-8"))
                self.patch_name = entry.filename.replace("_json.apmcmeta", "").replace(".apmcmeta", "")
                break
        manifest = self.get_manifest()
        if manifest.get("server") and not self.patch_data.get("server"):
            self._apply_server(manifest["server"])
        if manifest.get("password") and not self.patch_data.get("password"):
            self.patch_data["password"] = manifest["password"]

    def write_contents(self, opened_zipfile: zipfile.ZipFile) -> None:
        if self.server:
            self._apply_server(self.server)
        if self.password and not self.patch_data.get("password"):
            self.patch_data["password"] = self.password

        super().write_contents(opened_zipfile)
        filename = f"{self.patch_name}_json.apmcmeta" if self.patch_name else "patch_json.apmcmeta"
        opened_zipfile.writestr(filename, json.dumps(self.patch_data))

