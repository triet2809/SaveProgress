import { useEffect, useRef, useCallback } from 'react';
import { GOOGLE_CLIENT_ID } from '../../config/registerConfig';

// URL script cua Google Identity Services (GIS).
const GIS_SRC = 'https://accounts.google.com/gsi/client';

// Nap script GIS mot lan, tra ve promise resolve khi window.google san sang.
let gisPromise = null;
function loadGis() {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (gisPromise) return gisPromise;
  gisPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${GIS_SRC}"]`);
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', reject);
      return;
    }
    const script = document.createElement('script');
    script.src = GIS_SRC;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = reject;
    document.head.appendChild(script);
  });
  return gisPromise;
}

/**
 * Nut "Tiep tuc voi Google". Nap GIS, khoi tao voi GOOGLE_CLIENT_ID,
 * render nut chinh chu cua Google. Khi user chon tai khoan, GIS tra ve
 * credential (Google ID token) -> goi onCredential(idToken).
 *
 * @param {(idToken: string) => void} onCredential - callback nhan ID token.
 * @param {(msg: string) => void} onError - callback bao loi (tuy chon).
 * @param {string} text - kieu chu tren nut: 'signin_with' | 'signup_with' | 'continue_with'.
 */
const GoogleSignInButton = ({ onCredential, onError, text = 'continue_with' }) => {
  const containerRef = useRef(null);
  const onCredentialRef = useRef(onCredential);
  onCredentialRef.current = onCredential;

  const handleCredential = useCallback((response) => {
    if (response?.credential) {
      onCredentialRef.current?.(response.credential);
    }
  }, []);

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID) {
      onError?.('Google sign-in chua duoc cau hinh (VITE_GOOGLE_CLIENT_ID)');
      return;
    }
    let cancelled = false;
    loadGis()
      .then(() => {
        if (cancelled || !containerRef.current) return;
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleCredential,
        });
        window.google.accounts.id.renderButton(containerRef.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text,
          width: containerRef.current.offsetWidth || 320,
          logo_alignment: 'left',
        });
      })
      .catch(() => {
        if (!cancelled) onError?.('Khong tai duoc Google sign-in');
      });
    return () => { cancelled = true; };
  }, [handleCredential, onError, text]);

  return <div ref={containerRef} style={{ width: '100%', display: 'flex', justifyContent: 'center' }} />;
};

export default GoogleSignInButton;
