import React, { useEffect } from 'react'
import { useParams } from 'react-router-dom'

const ShortenUrlPage = () => {
    const { url } = useParams();

    useEffect(() => {
        if (url) {
            const backend = (import.meta.env.VITE_BACKEND_URL || "https://url-shortener-sb-g3if.onrender.com").replace(/\/$/, "");
            window.location.href = `${backend}/${url}`;
        }
    }, [url]);
  return <p>Redirecting...</p>;
}

export default ShortenUrlPage