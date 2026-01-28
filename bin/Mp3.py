#!/usr/bin/env python3
import tkinter as tk
from tkinter import ttk, filedialog, messagebox
import pygame
from pytube import YouTube
import os
import threading

class MP3App:
    def __init__(self, root):
        self.root = root
        self.root.title("MP3 Downloader & Player - Arch Linux")
        self.root.geometry("700x500")
        
        # Initialize pygame mixer
        pygame.mixer.init()
        
        self.setup_ui()
        self.create_dirs()
        
    def create_dirs(self):
        if not os.path.exists("downloads"):
            os.makedirs("downloads")
    
    def setup_ui(self):
        # Main container
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Download Section
        ttk.Label(main_frame, text="YouTube URL:").grid(row=0, column=0, sticky=tk.W)
        self.url_entry = ttk.Entry(main_frame, width=50)
        self.url_entry.grid(row=0, column=1, padx=5, pady=5)
        
        self.download_btn = ttk.Button(main_frame, text="Download MP3", command=self.download)
        self.download_btn.grid(row=0, column=2, padx=5)
        
        # Progress bar
        self.progress = ttk.Progressbar(main_frame, mode='indeterminate')
        self.progress.grid(row=1, column=0, columnspan=3, sticky=(tk.W, tk.E), pady=5)
        
        # Status label
        self.status = ttk.Label(main_frame, text="Ready")
        self.status.grid(row=2, column=0, columnspan=3, sticky=tk.W)
        
        # Playlist
        ttk.Label(main_frame, text="Downloaded MP3s:").grid(row=3, column=0, sticky=tk.W, pady=(20,5))
        
        # Listbox with scrollbar
        list_frame = ttk.Frame(main_frame)
        list_frame.grid(row=4, column=0, columnspan=3, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        self.listbox = tk.Listbox(list_frame, height=10)
        scrollbar = ttk.Scrollbar(list_frame, orient="vertical", command=self.listbox.yview)
        self.listbox.configure(yscrollcommand=scrollbar.set)
        
        self.listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        # Player controls
        controls_frame = ttk.Frame(main_frame)
        controls_frame.grid(row=5, column=0, columnspan=3, pady=10)
        
        ttk.Button(controls_frame, text="▶ Play", command=self.play).pack(side=tk.LEFT, padx=2)
        ttk.Button(controls_frame, text="⏸ Pause", command=self.pause).pack(side=tk.LEFT, padx=2)
        ttk.Button(controls_frame, text="⏹ Stop", command=self.stop).pack(side=tk.LEFT, padx=2)
        
        # Volume
        ttk.Label(controls_frame, text="Volume:").pack(side=tk.LEFT, padx=(20,5))
        self.volume = tk.Scale(controls_frame, from_=0, to=100, orient=tk.HORIZONTAL, length=100)
        self.volume.set(70)
        self.volume.pack(side=tk.LEFT)
        
        # Configure grid weights
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        
        self.refresh_list()
    
    def download(self):
        url = self.url_entry.get().strip()
        if not url:
            messagebox.showerror("Error", "Please enter a YouTube URL")
            return
        
        self.download_btn.config(state='disabled')
        self.progress.start()
        self.status.config(text="Downloading...")
        
        # Run download in thread
        thread = threading.Thread(target=self._download_thread, args=(url,))
        thread.daemon = True
        thread.start()
    
    def _download_thread(self, url):
        try:
            yt = YouTube(url)
            audio = yt.streams.filter(only_audio=True).first()
            
            # Download
            out_file = audio.download(output_path="downloads")
            
            # Convert to mp3 if needed
            base, ext = os.path.splitext(out_file)
            if ext != '.mp3':
                new_file = base + '.mp3'
                os.rename(out_file, new_file)
            
            # Update UI in main thread
            self.root.after(0, self._download_complete, yt.title)
            
        except Exception as e:
            self.root.after(0, self._download_error, str(e))
    
    def _download_complete(self, title):
        self.progress.stop()
        self.download_btn.config(state='normal')
        self.status.config(text=f"Downloaded: {title}")
        self.refresh_list()
        messagebox.showinfo("Success", "Download complete!")
    
    def _download_error(self, error):
        self.progress.stop()
        self.download_btn.config(state='normal')
        self.status.config(text=f"Error: {error}")
        messagebox.showerror("Error", f"Download failed:\n{error}")
    
    def refresh_list(self):
        self.listbox.delete(0, tk.END)
        if os.path.exists("downloads"):
            for file in sorted(os.listdir("downloads")):
                if file.endswith('.mp3'):
                    self.listbox.insert(tk.END, file)
    
    def play(self):
        selection = self.listbox.curselection()
        if not selection:
            messagebox.showwarning("Warning", "Please select a file to play")
            return
        
        filename = self.listbox.get(selection[0])
        filepath = os.path.join("downloads", filename)
        
        try:
            pygame.mixer.music.load(filepath)
            pygame.mixer.music.set_volume(self.volume.get() / 100)
            pygame.mixer.music.play()
            self.status.config(text=f"Playing: {filename}")
        except Exception as e:
            messagebox.showerror("Error", f"Cannot play file:\n{e}")
    
    def pause(self):
        if pygame.mixer.music.get_busy():
            pygame.mixer.music.pause()
            self.status.config(text="Paused")
    
    def stop(self):
        pygame.mixer.music.stop()
        self.status.config(text="Stopped")

if __name__ == "__main__":
    root = tk.Tk()
    app = MP3App(root)
    root.mainloop()