import { initializeApp } from "firebase/app";
import { getDatabase } from "firebase/database";
import { getAnalytics } from "firebase/analytics";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBtpZOklKB6Rj1_nzxnAgufP6eHWM6Ilec",
  authDomain: "hefimer.firebaseapp.com",
  databaseURL: "https://hefimer-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "hefimer",
  storageBucket: "hefimer.firebasestorage.app",
  messagingSenderId: "1020036475132",
  appId: "1:1020036475132:web:c109d721d35efd04cdcf79",
  measurementId: "G-LD3G3NWM47"
};

const app = initializeApp(firebaseConfig);
export const rtdb = getDatabase(app);
export const auth = getAuth(app);
export const analytics = typeof window !== 'undefined' ? getAnalytics(app) : null;
